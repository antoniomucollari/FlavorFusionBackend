package com.toni.FoodApp.restaurant.service;
import com.toni.FoodApp.analytics.service.AnalyticsService;
import com.toni.FoodApp.auth_users.dtos.SimpleUserDto;
import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.aws.AWSS3Service;
import com.toni.FoodApp.category.entity.RestaurantCategory;

import com.toni.FoodApp.category.repository.RestaurantCategoryRepository;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.NotFoundException;

import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.response.SerializablePage;
import com.toni.FoodApp.restaurant.dtos.*;
import com.toni.FoodApp.restaurant.dtos.response.RestaurantSummaryDTO;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.filterAndSpecification.BranchFilter;
import com.toni.FoodApp.restaurant.filterAndSpecification.BranchSpecifications;
import com.toni.FoodApp.restaurant.mapper.RestaurantDtoMapper;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import com.toni.FoodApp.restaurant.repository.RestaurantRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantServiceImpl implements RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final RestaurantCategoryRepository restaurantCategoryRepository;
    private final AWSS3Service awsS3Service;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;
    private final RestaurantDtoMapper restaurantDtoMapper;
    private final BranchRepository branchRepository;
    private final ModelMapper modelMapper;
    private final AnalyticsService analyticsService;
    private static final double MAX_SYSTEM_DELIVERY_RADIUS_KM = 8;
    private final RestaurantQueryCacheService restaurantQueryService;

    @Override
    public Response<?> createRestaurant(RestaurantRequestDto restaurantDTO) {
        log.info("Inside createRestaurant()");
        User owner = userService.getCurrentLoggedInUser();


        List<RestaurantCategory> categories = restaurantDTO.getCategories().stream()
                .map(id -> restaurantCategoryRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Category not found with id: " + id)))
                .collect(Collectors.toList());

        MultipartFile profileImageFile = restaurantDTO.getProfileImage();
        MultipartFile coverImageFile = restaurantDTO.getCoverImage();
        if (profileImageFile == null || profileImageFile.isEmpty() ||
                coverImageFile == null || coverImageFile.isEmpty()) {
            throw new IllegalArgumentException("Both cover and profile images are required.");
        }
        String profileImageUrl = awsS3Service.uploadImage(profileImageFile, "ProfileProfileImage");
        String coverImageUrl = awsS3Service.uploadImage(coverImageFile, "RestaurantCoverImage");

        Restaurant restaurant = Restaurant.builder()
                .name(restaurantDTO.getName())
                .description(restaurantDTO.getDescription())
                .profileImageUrl(profileImageUrl)
                .coverImageUrl(coverImageUrl)
                .categories(categories)
                .owner(owner)
                .phoneNumber(restaurantDTO.getPhoneNumber())
//                .isActive(true)
                .isPromoted(restaurantDTO.isPromoted())
                .build();
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        return Response.<RestaurantSummaryDTO>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Restaurant successfully created!")
                .build();
    }

    @Override
    public Response<Page<RestaurantSummaryDTO>> findAll(Pageable pageable) { //this is for customer on discovery page
        Page<Restaurant> entityPage = restaurantRepository.findAll(pageable);
        Page<RestaurantSummaryDTO> dtoPage = entityPage.map(restaurantDtoMapper::mapToSummaryDto);
        return Response.<Page<RestaurantSummaryDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Restaurants retrieved successfully.")
                .data(dtoPage)
                .build();
    }

    @Override
    public Response<Page<RestaurantsDto>> findAllForAdmin(Pageable pageable, Boolean deleted) {
        Page<RestaurantsDto> dtoPage = restaurantRepository.findRestaurantSummaries(pageable,deleted);
        return Response.<Page<RestaurantsDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Restaurants retrieved successfully.")
                .data(dtoPage)
                .build();
    }

    @Override
    public Response<?> deleteRestaurant(Long restaurantId) {

        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(()-> new EntityNotFoundException("Restaurant not found!"));
        if (Boolean.TRUE.equals(restaurant.getIsDeleted())) {
            throw new BadRequestException("Restaurant is already deleted!");
        }
        restaurant.setIsDeleted(true);
        restaurantRepository.save(restaurant);
        return Response.<List<SimpleUserDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Restaurant successfully deleted!")
                .build();
    }

    @Override
    public Response<?> unassignRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(()-> new EntityNotFoundException("Restaurant not found!"));
        validateIfRestaurantIsNotDeletedAndHasManager(restaurant);
        restaurant.setOwner(null);
        return Response.<List<SimpleUserDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Restaurant unassigned successfully!")
                .build();

    }
    @Override
    public Response<?> restoreRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(()-> new EntityNotFoundException("Restaurant not found!"));
        if (Boolean.FALSE.equals(restaurant.getIsDeleted())) {
            throw new BadRequestException("Restaurant is not deleted!");
        }
        restaurant.setIsDeleted(false);
        restaurantRepository.save(restaurant);
        return Response.<List<SimpleUserDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Restaurant successfully deleted!")
                .build();
    }



    private void validateIfRestaurantIsNotDeletedAndHasManager(Restaurant restaurant) {
        if (Boolean.TRUE.equals(restaurant.getIsDeleted())) {
            throw new BadRequestException("Restaurant is already deleted!");
        }
        if (restaurant.getOwner() == null) {
            throw new BadRequestException("Restaurant does not have a manager!");
        }
    }
    @Override
    public Response<RestaurantSummaryDTO> findByRestaurant(Long id) {

        // 1. Fetch the restaurant directly from the database (bypassing Redis location filters)
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Restaurant not found with ID: " + id));
        validateIfRestaurantIsNotDeletedAndHasManager(restaurant);

        RestaurantSummaryDTO restaurantDto = restaurantDtoMapper.mapToSummaryDto(restaurant);
        return Response.<RestaurantSummaryDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Successfully fetched restaurant branches")
                .data(restaurantDto)
                .build();
    }
    @Override
    public Page<RestaurantSummaryDTO> findAvailableRestaurants(
            RestaurantFilterCriteria criteria,
            Pageable pageable,
            String sort,
            Double lat,
            Double lng
    ) {

        User customer = userService.getCurrentLoggedInUserOrNull();

        Double exactLat; // user lat
        Double exactLng;

        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
        String redisKey = "restaurant:locations";

        if (customer != null) {
            if (customer.getDeliveryLocation() == null) {
                throw new BadRequestException("Please set an active delivery address before searching.");
            }
            exactLat = customer.getDeliveryLocation().getLatitude();
            exactLng = customer.getDeliveryLocation().getLongitude();
        } else {
            exactLng = lng;
            exactLat = lat;
            // Guest user: Must provide coordinates via RequestParams
            if (exactLat == null || exactLng == null) {
                throw new BadRequestException("Please provide a delivery location to see available restaurants.");
            }
        }

        log.info("Inside findAvailableRestaurants() lat:{} lng:{}", exactLat, exactLng);

        // --- 1. Query REDIS for physical distance ---
        Distance searchRadius = new Distance(
                MAX_SYSTEM_DELIVERY_RADIUS_KM, RedisGeoCommands.DistanceUnit.KILOMETERS
        );
        org.springframework.data.geo.Point userPoint = new org.springframework.data.geo.Point(exactLng, exactLat);
        Circle searchCircle = new Circle(userPoint, searchRadius);
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
                geoOps.radius(redisKey, searchCircle);
        if (geoResults == null || geoResults.getContent().isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> nearbyBranchIds = geoResults.getContent().stream()
                .map(result -> Long.parseLong(result.getContent().getName()))
                .collect(Collectors.toList());

        Pageable repoPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());


        String safeSort = (sort != null) ? sort : "default";

        SerializablePage<RestaurantSummaryDTO> cachedResult = restaurantQueryService.getCachedDashboardPage(
                safeSort,
                repoPageable,
                exactLat,
                exactLng,
                nearbyBranchIds,
                criteria
        );

        return cachedResult.toSpringPage();
    }

    @Override
    public Response<List<SimpleBranchDto>> findBranchesForRestaurant(BranchFilter filter) {

        // We delegate to the new location
        Long restaurantId = userService.getRestaurantIdByCurrentLoggedUser().getId();

        List<RestaurantBranch> branches = branchRepository.findAll(
                BranchSpecifications.forRestaurant(restaurantId, filter)
        );

        List<SimpleBranchDto> branchesDto = modelMapper.map(
                branches,
                new org.modelmapper.TypeToken<List<SimpleBranchDto>>(){}.getType()
        );


        return Response.<List<SimpleBranchDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Successfully fetched restaurant branches")
                .data(branchesDto)
                .build();
    }

    @Override
    public Response<SimpleRestaurantDto> getCurrentRestaurant() {
        // We delegate to the new location
        Long restaurantId = userService.getRestaurantIdByCurrentLoggedUser().getId();
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(() -> new NotFoundException("Restaurant not found"));
        SimpleRestaurantDto restaurantDto = modelMapper.map(restaurant, SimpleRestaurantDto.class);
        List<RestaurantCategory> restaurantCategories = restaurant.getCategories();

        List<SimpleRestaurantDto.CategoryInfo> mappedCategories = restaurantCategories.stream()
                .map(category -> new SimpleRestaurantDto.CategoryInfo(category.getId(), category.getName()))
                .toList();
        restaurantDto.setCategories(mappedCategories);

        return Response.<SimpleRestaurantDto>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Successfully fetched restaurant branches")
                .data(restaurantDto)
                .build();
    }


}
