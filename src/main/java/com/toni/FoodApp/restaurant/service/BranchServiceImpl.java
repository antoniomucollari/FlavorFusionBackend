package com.toni.FoodApp.restaurant.service;

import com.toni.FoodApp.auth_users.dtos.SimpleUserDto;
import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.repository.UserRepository;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.category.dtos.CategoryDTO;
import com.toni.FoodApp.category.entity.Category;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.ForbiddenException;
import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.menu.entity.Menu;
import com.toni.FoodApp.menu.service.CachedMenuService;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.payment.repository.PaymentMethodRepository;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.dtos.*;
import com.toni.FoodApp.restaurant.entity.BranchMenuItem;
import com.toni.FoodApp.restaurant.entity.OpeningHour;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.mapper.RestaurantBranchMapper;
import com.toni.FoodApp.restaurant.repository.*;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.modelmapper.ModelMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final OpeningHoursRepository openingHourRepository;
    private final ModelMapper modelMapper;
    private final org.locationtech.jts.geom.GeometryFactory geometryFactory;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String REDIS_GEO_KEY = "restaurant:locations";
    private final RestaurantBranchMapper restaurantBranchMapper;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PaymentMethodRepository paymentMethodRepository;
    private final RestaurantQueryCacheService restaurantQueryCacheService;
    private final CacheManager cacheManager;
    private final CachedMenuService cachedMenuService;

    @Override
    @Transactional
    public Response<?> saveOpeningHours(OpeningHoursRequestDto dto) {
        RestaurantBranch branch = getBranchFromCurrentUser();
        openingHourRepository.deleteByBranch(branch);
        if (branch == null) {
            throw new NotFoundException("No branch found for current user");
        }
        List<OpeningHour> newOpeningHours = dto.getOpeningHours().stream()
                .map(hourDto ->
                        // Use the @Builder from your OpeningHour entity
                        OpeningHour.builder()
                                .dayOfWeek(hourDto.getDayOfWeek())
                                .openTime(hourDto.getOpenTime())
                                .closeTime(hourDto.getCloseTime())
                                .branch(branch)
                                .build()
                )
                .collect(Collectors.toList());

        openingHourRepository.saveAll(newOpeningHours);

        // 5. Return the success response
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Opening hours set successfully.") // "Set" is more accurate than "Added"
                .build();
    }

    @CacheEvict(value = "restaurantDetails", key = "#branchId")
    @Override
    public Response<?> editBranch(Long branchId, BranchRequestDto branchDto) {

        RestaurantBranch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found with id: " + branchId));

        belongsToRestaurant(branch.getRestaurant().getId());

        modelMapper.map(branchDto, branch);

        Point branchLocation = geometryFactory.createPoint(
                new Coordinate(branchDto.getLongitude(), branchDto.getLatitude())
        );
        branch.setLocation(branchLocation);
        branch.setDeliveryRadiusInKm(branchDto.getDeliveryRadiusInKm());

        branchRepository.save(branch);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch updated successfully.")
                .build();
    }

    @Override
    public Response<?> editMyBranch(BranchOperationsRequest branchDto) {
        RestaurantBranch branch = getBranchFromCurrentUser();
        modelMapper.map(branchDto, branch);
        branchRepository.save(branch);
        updateRedisGeoIndex(branch);
        var cache = cacheManager.getCache("restaurantDetails");
        if (cache != null) {
            cache.evict(branch.getId());
            log.info("Evicted restaurantDetails cache for branch ID: {}", branch.getId());
        }
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch updated successfully.")
                .build();
    }

    @Override
    public Response<?> createBranch(BranchRequestDto branchDto) {

        Point branchLocation = geometryFactory.createPoint(
                new Coordinate(branchDto.getLongitude(), branchDto.getLatitude()));


        RestaurantBranch branch = modelMapper.map(branchDto, RestaurantBranch.class);

        Restaurant restaurant = userService.getCurrentLoggedInUser().getRestaurant();
        branch.setRestaurant(restaurant);

        branch.setLocation(branchLocation);

        branchRepository.save(branch);
        updateRedisGeoIndex(branch);

        return Response.<BranchRequestDto>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Restaurant Branch successfully created!")
                .build();
    }

    private void updateRedisGeoIndex(RestaurantBranch branch){
        if (branch.getLocation() != null) {
            var jtsPoint = branch.getLocation();
            var redisPoint = new org.springframework.data.geo.Point(
                    jtsPoint.getX(),
                    jtsPoint.getY()
            );
            redisTemplate.opsForGeo().add(
                    REDIS_GEO_KEY,
                    new RedisGeoCommands.GeoLocation<>(
                            String.valueOf(branch.getId()),
                            redisPoint
                    )
            );
        }
    }
    @Override
    public Response<RestaurantBranchDetailsDto> getById(Long id) {
        log.info("Inside getBranchById for id: {}", id);
        return buildBranchDetailsResponse(id);
    }


    @Override
    public Response<RestaurantBranchDetailsDto> getMyBranch() {
        Long branchId = getBranchFromCurrentUser().getId();
        log.info("Inside getMyBranch for branch id: {}", branchId);
        return buildBranchDetailsResponse(branchId);
    }
    private Response<RestaurantBranchDetailsDto> buildBranchDetailsResponse(Long branchId) {

        // This cross-bean call triggers the Redis AOP Proxy!
        RestaurantBranchDetailsDto dto = restaurantQueryCacheService.getBranchDetails(branchId);

        return Response.<RestaurantBranchDetailsDto>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch details retrieved successfully")
                .data(dto)
                .build();
    }

    @Override
    public RestaurantBranch getBranchFromCurrentUser() {
        User currentUser = userService.getCurrentLoggedInUser();

        if (currentUser.getManagedBranch() == null) {
            throw new NotFoundException("Current user does not have a managed branch assigned.");
        }
        return currentUser.getManagedBranch();
    }

    @Override
    public Response<BranchLocationDto> getBranchLocation(Long orderId) {
        RestaurantBranch branch = branchRepository.findByOrderId(orderId).orElseThrow(()-> new EntityNotFoundException("Branch not found"));
        BranchLocationDto dto = BranchLocationDto.builder()
                .id(branch.getId())
                .address(branch.getAddress())
                .latitude(branch.getLocation().getY())
                .longitude(branch.getLocation().getX())
                .build();

        return Response.<BranchLocationDto>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch location retrieved successfully")
                .data(dto)
                .build();
    }

    @Override
    public Response<Boolean> changeOpeningStatus() {
        RestaurantBranch branch = getBranchFromCurrentUser();
        boolean newStatus = !branch.isClosed();
        branch.setClosed(newStatus);
        var cache = cacheManager.getCache("restaurantDetails");
        if (cache != null) {
            cache.evict(branch.getId());
            log.info("Evicted cache for closed store. Branch ID: {}", branch.getId());
        }
        branchRepository.save(branch);
        return Response.<Boolean>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch details retrieved successfully")
                .data(newStatus)
                .build();

    }



    @Transactional(readOnly = true)
    public Response<List<CategoryDTO>> getMenusByBranchId(Long branchId, String searchString) {

        if (!branchRepository.existsById(branchId)) {
            throw new BadRequestException("Branch not found");
        }

       //Fetch the FULL menu from Redis (or DB if cache miss)
        List<CategoryDTO> fullMenu = cachedMenuService.getFullMenu(branchId);

        List<CategoryDTO> finalMenu = fullMenu;

        // if search string than do a search
        if (searchString != null && !searchString.trim().isEmpty()) {
            String query = searchString.trim().toLowerCase();

            finalMenu = fullMenu.stream()
                    .map(category -> {
                        // Filter the items inside this category
                        List<MenuItemDto> filteredItems = category.getMenus().stream()
                                .filter(item -> item.getName().toLowerCase().contains(query) ||
                                        (item.getDescription() != null && item.getDescription().toLowerCase().contains(query)))
                                .collect(Collectors.toList());


                        return CategoryDTO.builder()
                                .id(category.getId())
                                .name(category.getName())
                                .menus(filteredItems)
                                .build();
                    })
                    //Remove any categories that ended up empty
                    .filter(category -> !category.getMenus().isEmpty())
                    .collect(Collectors.toList());
        }

        return Response.<List<CategoryDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu retrieved successfully")
                .data(finalMenu)
                .build();
    }

    @Override
    public Response<?> changeManager(Long branchId, Long userId) {
        RestaurantBranch branch = branchRepository.findById(branchId).orElseThrow(()-> new EntityNotFoundException("Branch not found"));
        //make sure the given branchId belongs to the logged in restaurant manager
        Restaurant restaurantRequest = branch.getRestaurant();
        belongsToRestaurant(restaurantRequest.getId());


        if(userId == null){
            branch.setManager(null);
        }
        else{
            User user = userRepository.findById(userId).orElseThrow(()-> new EntityNotFoundException("User not found"));
            if (branch.getManager() != null &&
                    Objects.equals(user.getId(), branch.getManager().getId())) {
                throw new BadRequestException("You cannot change the manager to the same user");
            }
            branch.setManager(user);
        }
        branchRepository.save(branch);

        return Response.<List<SimpleUserDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Manager changed successfully")
                .build();
    }

    @Override
    public Response<?> deleteBranch(Long branchId) {

        RestaurantBranch branch = branchRepository.findById(branchId).orElseThrow(()-> new EntityNotFoundException("Branch not found!"));
        if (Boolean.TRUE.equals(branch.getDeleted())) {
            throw new BadRequestException("Branch is already deleted!");
        }
        belongsToRestaurant(branch.getRestaurant().getId());
        branch.setClosed(true);
        branch.setDeleted(true);
        branchRepository.save(branch);

        return Response.<List<SimpleUserDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch successfully deleted!")
                .build();

    }



    @Override
    public Response<?> restoreBranch(Long branchId) {
        RestaurantBranch branch = branchRepository.findById(branchId).orElseThrow(()-> new EntityNotFoundException("Branch not found!"));
        if (!Boolean.TRUE.equals(branch.getDeleted())) {
            throw new BadRequestException("Branch is not deleted to perform restore operation!");
        }
        belongsToRestaurant(branch.getRestaurant().getId());
        branch.setDeleted(false);
        branchRepository.save(branch);
        return Response.<List<SimpleUserDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch successfully restored!")
                .build();
    }


    private void belongsToRestaurant(Long restaurantId){
        Restaurant currentUserRestaurant = userService.getCurrentLoggedInUser().getRestaurant();

        if (! Objects.equals(restaurantId, currentUserRestaurant.getId())){
            throw new ForbiddenException("You can only make changes for your own restaurant branches.");
        }
    }

    @Override
    public void assignBranchManagerAndValidate(Long branchId, User manager){
        RestaurantBranch branch = branchRepository.findById(branchId).orElseThrow(()-> new NotFoundException("Branch not found with id: " + branchId));
        if (branch.getManager() != null) throw new BadRequestException("Branch already has a manager");
        //make sure the branch belong to the manager
        belongsToRestaurant(branch.getRestaurant().getId());
        branch.setManager(manager);
        branchRepository.save(branch);
    }


    //--------------------------------end manager panel ------------------------

//todo add a dedicated rebuildgeoindex

//    @PostConstruct
//    public void rebuildGeoIndexOnStartup() {
//        log.info("--- Rebuilding Redis index on startup ---");
//        List<RestaurantBranch> allBranches = branchRepository.findAll();
//
//        if (allBranches.isEmpty()) {
//            System.out.println("No branches found in database. Index will be empty.");
//            return;
//        }
//
//        redisTemplate.delete(REDIS_GEO_KEY);
//
//        var geoOps = redisTemplate.opsForGeo();
//
//        var locations = allBranches.stream()
//                .filter(branch -> branch.getLocation() != null)
//                .map(branch -> {
//                    var jtsPoint = branch.getLocation(); // org.locationtech.jts.geom.Point
//                    var redisPoint = new org.springframework.data.geo.Point(
//                            jtsPoint.getX(), // longitude
//                            jtsPoint.getY()  // latitude
//                    );
//
//                    return new RedisGeoCommands.GeoLocation<>(
//                            String.valueOf(branch.getId()),
//                            redisPoint
//                    );
//                })
//                .collect(Collectors.toList());
//
//        geoOps.add(REDIS_GEO_KEY, locations);
//
//        System.out.println("--- Successfully rebuilt Redis index with " + locations.size() + " branches. ---");
//    }



    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodEntity> getAvailablePaymentMethods(Long branchId) {
        return paymentMethodRepository.findPaymentMethodsByBranchIdOrderById(branchId);
        //depricated
//        RestaurantBranch branch = branchRepository.findById(branchId)
//                .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + branchId));
//        return branch.getPaymentMethods()
//                .stream().sorted(Comparator.comparing(PaymentMethodEntity::getId)).toList();
    }

    @Override
    public Boolean subtotalIsHigherThanMinOrderAmount(Long branchId, BigDecimal subtotal){
        RestaurantBranch branch = branchRepository.findById(branchId).orElseThrow(() -> new EntityNotFoundException("Branch not found: " + branchId));
        return !(branch.getMinOrderAmount() > subtotal.doubleValue());
    }

    @Override
    public RestaurantBranch getBranchEntityById(Long branchId) {
        return branchRepository.findById(branchId).orElseThrow(()-> new EntityNotFoundException("Branch not found"));
    }

    @Override
    public List<RestaurantBranch> getBranchesByRestaurantId(Long restaurantId) {
        return branchRepository.findByRestaurantId(restaurantId);
    }


}