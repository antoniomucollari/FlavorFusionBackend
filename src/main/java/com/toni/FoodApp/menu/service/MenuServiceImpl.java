package com.toni.FoodApp.menu.service;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.aws.AWSS3Service;
import com.toni.FoodApp.cart.repository.CartItemVariantRepository;
import com.toni.FoodApp.category.dtos.SimpleCategoryDto;
import com.toni.FoodApp.category.entity.Category;
import com.toni.FoodApp.category.repository.CategoryRepository;
import com.toni.FoodApp.enums.RoleName;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.ForbiddenException;
import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.menu.dtos.*;
import com.toni.FoodApp.menu.entity.Menu;
import com.toni.FoodApp.menu.entity.OptionGroup;
import com.toni.FoodApp.menu.entity.OptionVariant;
import com.toni.FoodApp.menu.mapper.MenuMapper;
import com.toni.FoodApp.menu.repository.MenuRepository;
import com.toni.FoodApp.menu.repository.OptionRepository;
import com.toni.FoodApp.menu.repository.OptionVariantRepository;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.entity.BranchMenuItem;
import com.toni.FoodApp.restaurant.entity.BranchOptionConfig;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.repository.BranchConfigRepository;
import com.toni.FoodApp.restaurant.repository.BranchMenuItemRepository;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuServiceImpl implements MenuService{
    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final AWSS3Service awsS3Service;
    private final UserService userService;
    private final OptionRepository optionRepository;
    private final MenuMapper menuMapper;
    private final CartItemVariantRepository cartItemVariantRepository;
    private final BranchMenuItemRepository branchMenuItemRepository;
    private final BranchConfigRepository branchConfigRepository;
    private final OptionVariantRepository optionVariantRepository;
    private final BranchRepository branchRepository;
    private Menu getMenuByIdOrThrow(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Menu not found with id: " + id));
    }

    @Override
    public Response<MenuDTO> createMenu(CreateMenuRequest menuDTO) {
        log.info("Inside createMenu()");

        Category category = categoryRepository.findById(menuDTO.getCategoryId())
                .orElseThrow(()-> new NotFoundException("Category not found"));

        MultipartFile imageFile = menuDTO.getImageFile();
        if(imageFile == null || imageFile.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        String imageUrl = awsS3Service.uploadImage(imageFile, "menus");;

        Menu menu = Menu.builder()
                .name(menuDTO.getName())
                .description(menuDTO.getDescription())
                .imageUrl(imageUrl)
                .category(category)
                .build();

        Menu savedMenu = menuRepository.save(menu);

            return Response.<MenuDTO>builder()
                    .statusCode(HttpStatus.CREATED.value())
                    .message("Menu successfully created!")
                    .data(modelMapper.map(savedMenu, MenuDTO.class))
                    .build();
    }

    @Override
    public Response<CreateMenuRequest> updateMenu(CreateMenuRequest menuDTO) {
        log.info("Inside updateMenu()");
        Menu existingMenu = getMenuByIdOrThrow(menuDTO.getId());
        Long categoryId = menuDTO.getCategoryId();
        if(menuDTO.getImageFile() == null && existingMenu.getImageUrl() == null) {
            throw new BadRequestException("Image file is required");
        }
        if (categoryId == null) {
            throw new NotFoundException("Category not found");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        MultipartFile imageFile = menuDTO.getImageFile();

        String newImageUrl = awsS3Service.replaceImage(imageFile, existingMenu.getImageUrl(), "menus");
        if(StringUtils.hasText(menuDTO.getName())) existingMenu.setName(menuDTO.getName());
        if(StringUtils.hasText(menuDTO.getDescription())) existingMenu.setDescription(menuDTO.getDescription());

        existingMenu.setImageUrl(newImageUrl);
        existingMenu.setCategory(category);
        Menu updatedMenu = menuRepository.save(existingMenu);
        return Response.<CreateMenuRequest> builder()
                .statusCode(HttpStatus.OK.value())
                .message("Account updated successfully")
                .data(modelMapper.map(updatedMenu, CreateMenuRequest.class))
                .build();
    }

    @Override
    public Response<MenuResponse> getMenuByBranchId(Long branchMenuId) {
        BranchMenuItem branchMenu = branchMenuItemRepository.findById(branchMenuId).orElseThrow(()->new NotFoundException("Not found."));
        Menu existingMenu = branchMenu.getMenu();
        MenuResponse menuResponse = modelMapper.map(existingMenu, MenuResponse.class);
        if (existingMenu.getCategory() != null) {
            menuResponse.setCategory(modelMapper.map(existingMenu.getCategory(), SimpleCategoryDto.class));
        }
        BigDecimal price = branchMenu.getPrice() ;
        menuResponse.setPrice(price);
        return Response.<MenuResponse> builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu retrieved successfully")
                .data(menuResponse)
                .build();
    }

    @Override
    public Response<MenuDTO> deleteMenu(Long id) {
        Menu menuToDelete = getMenuByIdOrThrow(id);

        String imageUrl = menuToDelete.getImageUrl();
        if (StringUtils.hasText(imageUrl)) {
            String keyName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            awsS3Service.deleteFile("menus/" +keyName);
        }
        menuRepository.delete(menuToDelete);

        return Response.<MenuDTO> builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu deleted successfully")
                .build();
    }

    @Override
    public Response<List<MenuDTO>> getMenus(Long categoryId, String searchTerm,Boolean myMenus) {
        log.info("inside getMenus()");
        Specification<Menu> specs = buildSpecification(categoryId, searchTerm, myMenus);

        List<Menu> menuList = menuRepository.findAll(specs, Sort.by(Sort.Direction.DESC, "id"));

        List<MenuDTO> menuDTOS = menuList.stream()
                .map(menu -> {
                    MenuDTO dto = modelMapper.map(menu, MenuDTO.class);

                    Category category = menu.getCategory();
                    if (category != null) {
                        SimpleCategoryDto categoryDTO = new SimpleCategoryDto();
                        categoryDTO.setId(category.getId());
                        categoryDTO.setName(category.getName());
                        categoryDTO.setDeleted(category.getDeleted());
                        dto.setCategory(categoryDTO);
                    }
                    return dto;
                })
                .toList();


        return Response.<List<MenuDTO>> builder()
                .statusCode(HttpStatus.OK.value())
                .data(menuDTOS)
                .message("Menu retrieved successfully")
                .build();
    }

    @Override
    public Response<?> createMenuOption(OptionGroupDTO optionDTO, Long menuId) {

        //check if menu exist
        Menu menu = getMenuByIdOrThrow(menuId);
        //check if the menu belogs to manager
        menuBelongToManager(menuId);
        Restaurant restaurant = userService.getRestaurantIdByCurrentLoggedUser();
        //add the option
        OptionGroup optionGroup = menuMapper.mapToOptionGroup(optionDTO, restaurant);
        menu.getOptionGroups().add(optionGroup);
            menuRepository.save(menu); //CascadeType.ALL in menu
        //save
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Option created successfully for menu with id " + menuId)
                .build();
    }

    @Override
    @Transactional
    public Response<?> editMenuOption(OptionGroupDTO optionDTO) {
        //check if optionExist
        OptionGroup existingGroup = optionRepository.findById(optionDTO.getId())
                .orElseThrow(() -> new NotFoundException("Option Group not found"));
        Restaurant restaurant = userService.getRestaurantIdByCurrentLoggedUser();
        //check if this option can be accessed by manager
        if (!existingGroup.getRestaurant().getId().equals(restaurant.getId())) {
            throw new ForbiddenException("Not allowed! This option belongs to another restaurant.");
        }
        if(optionDTO.getName() == null || optionDTO.getVariants().equals(new ArrayList<>())){
            throw new BadRequestException("Name and variants cant be null");
        }
        existingGroup.setName(optionDTO.getName());
        existingGroup.setMinSelection(optionDTO.getMinSelection());
        existingGroup.setMaxSelection(optionDTO.getMaxSelection());
        updateVariants(existingGroup, optionDTO.getVariants());
        //add the option
        optionRepository.save(existingGroup);
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Option created successfully")
                .build();
    }

    @Override
    @Transactional
    public Response<?> deleteOption(Long optionId) {
        // Validations
        OptionGroup existingGroup = optionRepository.findById(optionId)
                .orElseThrow(() -> new NotFoundException("Option Group not found"));
        Restaurant currentRestaurant = userService.getRestaurantIdByCurrentLoggedUser();
        if (!existingGroup.getRestaurant().getId().equals(currentRestaurant.getId())) {
            throw new ForbiddenException("Not allowed! This option belongs to another restaurant.");
        }
        boolean isUsedInCarts = cartItemVariantRepository.isOptionGroupInUse(optionId);
        //TODO isUsedInConfigs
        if (isUsedInCarts /* || isUsedInConfigs */) {
            existingGroup.setDeleted(true);
            optionRepository.save(existingGroup);

            return Response.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Option is currently in use by customers. Soft deleted successfully.")
                    .build();
        } else {
            optionRepository.unlinkFromAllMenus(optionId);
            optionRepository.delete(existingGroup);

            return Response.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Option was unused. Permanently deleted.")
                    .build();
        }
    }


    private void updateVariants(OptionGroup existingGroup, List<VariantDTO> incomingVariants) {
        // 1. Map incoming DTOs by ID for fast lookup
        Map<Long, VariantDTO> incomingMap = incomingVariants.stream()
                .filter(v -> v.getId() != null)
                .collect(Collectors.toMap(VariantDTO::getId, Function.identity()));

        List<OptionVariant> currentVariants = existingGroup.getVariants();

        // 2. LOOP 1: Update Existing items & Mark missing ones as DELETED
        for (OptionVariant variant : currentVariants) {
            if (incomingMap.containsKey(variant.getId())) {
                // Case A: The user kept this variant -> Update it
                VariantDTO dto = incomingMap.get(variant.getId());
                variant.setName(dto.getName());
                variant.setRecommendedPrice(dto.getRecommendedPrice());
                variant.setDeleted(false); // Reactivate it just in case it was previously soft-deleted
            } else {
                variant.setDeleted(true);
            }
        }

        // 3. LOOP 2: Add NEW items (Items with null ID)
        List<OptionVariant> newVariants = new ArrayList<>();
        for (VariantDTO dto : incomingVariants) {
            if (dto.getId() == null) {
                // Case C: New Variant -> Create it
                OptionVariant newVariant = OptionVariant.builder()
                        .name(dto.getName())
                        .recommendedPrice(dto.getRecommendedPrice())
                        .group(existingGroup) // Important: Link to parent
                        .isDeleted(false)
                        .build();
                newVariants.add(newVariant);
            }
        }

        currentVariants.addAll(newVariants);
    }


    @Override
    public Response<List<OptionGroupDTO>> getAllOptions(Long menuId) {
        Menu menu = getMenuByIdOrThrow(menuId);
        menuBelongToManager(menuId);
        List<OptionGroup> optionGroups = menu.getOptionGroups();
        List<OptionGroupDTO> dto = optionGroups.stream().map(
                optionGroup ->
                        menuMapper.mapToOptionGroupDto(optionGroup, menuMapper.mapToListVariantDto(optionGroup.getVariants()))
                )
                .toList();
        return Response.<List<OptionGroupDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Options successfully fetched.")
                .data(dto)
                .build();
    }

    @Override
    public Response<List<BranchOptionGroupDto>> getAllOptionsBranch(Long menuId) {
        RestaurantBranch branch = userService.getCurrentLoggedInUser().getManagedBranch();
        Menu menu = getMenuByIdOrThrow(menuId);

        //get all options for the menu
        List<OptionGroup> globalGroups = menu.getOptionGroups();

        //get all option configs for branch
        List<BranchOptionConfig> branchConfigs = branchConfigRepository.findByBranchId(branch.getId());

        // hashmap for branch variants with id as key
        Map<Long, BranchOptionConfig> configMap = branchConfigs.stream()
                .collect(Collectors.toMap(
                        config -> config.getVariant().getId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        // Logic is here to tell if branch has overwritten a variant or not and return the final dto
        List<BranchOptionGroupDto> dtos = globalGroups.stream()
                .map(group -> menuMapper.mapToBranchGroupDto(group, configMap))
                .toList();

        return Response.<List<BranchOptionGroupDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch options fetched successfully.")
                .data(dtos)
                .build();
    }
        //TOFIX
    @Override
    public Response<List<OptionGroupDTO>> getAllOptionsCustomer(Long branchMenuId, Long branchId) {
        //get the menuId from the branchMenuId param
        log.info("inside get option");
        // 1. Fetch the flat, fully-filtered data directly from the DB
        List<MenuOptionFlatDTO> flatOptions = optionRepository.findAvailableOptionsRaw(branchMenuId, branchId);

        // 2. Let the mapper handle the grouping and DTO creation
        List<OptionGroupDTO> dto = menuMapper.mapFlatOptionsToOptionGroupDtos(flatOptions);

        // 3. Return response
        return Response.<List<OptionGroupDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch options fetched successfully.")
                .data(dto)
                .build();
    }
    private List<MenuOptionFlatDTO> filterOutUnAvailableVariants(Long menuId, Long branchId){

        return optionRepository.findAvailableOptionsRaw(menuId, branchId);
    };

/*
depricated
    private VariantDTO getEffectiveVariant(Long variantId, Long branchId){
        //fetch the manager variant
        OptionVariant globalVariant = optionVariantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("Variant not found"));
        //check if there is a branchconfig for the variant
        BranchOptionConfig branchOptionConfig = branchConfigRepository.findByBranchIdAndVariantId(branchId,variantId).orElse(null);
        //if there is than return the variant dto with the branchconfig data
        if(branchOptionConfig != null){
            return VariantDTO.builder()
                    .isAvailable(!Boolean.FALSE.equals(branchOptionConfig.getIsAvailable()))
                    .name(globalVariant.getName())
                    .isDeleted(globalVariant.isDeleted())
                    .id(globalVariant.getId())
                    .recommendedPrice(branchOptionConfig.getPriceOverride())
                    .build();
        }
        //if not than map to dto the variantOption entity and return
        else {
            return menuMapper.mapToVariantDTO(globalVariant);
        }
    }
*/
    @Override
    @Transactional
    public Response<?> overwriteMenuOptions(List<BranchConfigUpdateRequest> requests, Long optionId) {
        // 1. Get the Branch Reference (The Manager's Branch)
        RestaurantBranch branchReference = userService.getCurrentLoggedInUser().getManagedBranch();

        // 2. Collect all Variant IDs from the request list
        List<Long> variantIds = requests.stream()
                .map(BranchConfigUpdateRequest::getVariantId)
                .toList();

        // 3. OPTIMIZATION: Fetch all needed OptionVariants in ONE query
        // We need these objects to set the foreign key: config.setVariant(variantObject)
        List<OptionVariant> variants = optionVariantRepository.findAllById(variantIds);

        // Map them for instant lookup: ID -> Variant Object
        Map<Long, OptionVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(OptionVariant::getId, Function.identity()));

        // 4. Fetch existing configs (using the fixed repository method)
        List<BranchOptionConfig> existingConfigs = branchConfigRepository
                .findByBranchIdAndVariantIdIn(branchReference.getId(), variantIds);

        // 5. Loop through requests
        for (BranchConfigUpdateRequest req : requests) {
            // Find existing config or create NEW
            BranchOptionConfig config = existingConfigs.stream()
                    .filter(c -> c.getVariant().getId().equals(req.getVariantId()))
                    .findFirst()
                    .orElse(new BranchOptionConfig());

            // --- Handling New Configs ---
            if (config.getId() == null) {
                // This is where we use the references!

                // A. Set Branch
                config.setBranch(branchReference);

                // B. Set Variant (Look it up from our map)
                OptionVariant variantReference = variantMap.get(req.getVariantId());
                if (variantReference == null) {
                    throw new NotFoundException("Variant with ID " + req.getVariantId() + " not found.");
                }
                config.setVariant(variantReference);

                // Set default availability if new
                if (req.getIsAvailable() == null) config.setIsAvailable(true);
            }

            // --- Updating Values ---
            if (req.getPrice() != null) {
                config.setPriceOverride(req.getPrice());
            }
            if (req.getIsAvailable() != null) {
                config.setIsAvailable(req.getIsAvailable());
            }

            branchConfigRepository.save(config);
        }

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Updated " + requests.size() + " variant configurations.")
                .build();
    }


    @Override
    public Response<List<OptionGroupDTO>> getAllOptionsForRestaurant() {
        Long restaurantId = userService.getRestaurantIdByCurrentLoggedUser().getId();
        List<OptionGroup> optionGroups = optionRepository.findByRestaurantId(restaurantId);
        List<OptionGroupDTO> dto = optionGroups.stream().map(optionGroup ->
                menuMapper.mapToOptionGroupDto(optionGroup, menuMapper.mapToListVariantDto(optionGroup.getVariants())
                )).toList();
        return Response.<List<OptionGroupDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Options successfully fetched.")
                .data(dto)
                .build();
    }

    @Override
    public Response<List<OptionGroupDTO>> getAvailableOptions(Long menuId) {
        getMenuByIdOrThrow(menuId);
        menuBelongToManager(menuId);
        Long restaurantId = userService.getRestaurantIdByCurrentLoggedUser().getId();

        List<OptionGroup> availableGroups = optionRepository.findAvailableOptionsForMenu(restaurantId, menuId);

        List<OptionGroupDTO> dto = availableGroups.stream()
                .map(singleOption -> menuMapper.mapToOptionGroupDto(singleOption, menuMapper.mapToListVariantDto(singleOption.getVariants())))
                .toList();

        return Response.<List<OptionGroupDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Available options successfully fetched.")
                .data(dto)
                .build();
    }



    @Override
    public Response<?> unlinkOptionFromMenu(Long menuId, Long optionId) {
        menuAndOptionValidation(menuId, optionId);
        Menu menu = getMenuByIdOrThrow(menuId);
        boolean removed = menu.getOptionGroups().removeIf(group -> group.getId().equals(optionId));

        if (!removed) {
            throw new NotFoundException("This option was not linked to this menu.");
        }
        menuRepository.save(menu);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Option successfully unlinked from menu.")
                .build();
    }
    private void menuAndOptionValidation(Long menuId, Long optionId){
        //validations
        menuBelongToManager(menuId);
        //option belong to the restaurant
        Long restaurantId = userService.getRestaurantIdByCurrentLoggedUser().getId();
        boolean isOwner =  optionRepository.existsByIdAndRestaurantId(optionId, restaurantId);
        if (!isOwner) {
            throw new ForbiddenException("You do not have permission to modify this option group.");
        }

    }

    @Override
    public Response<?> linkOptionFromMenu(Long menuId, Long optionId) {
        menuAndOptionValidation(menuId,optionId);
        Menu menu = getMenuByIdOrThrow(menuId);
        OptionGroup optionGroup = optionRepository.findById(optionId).orElseThrow(() -> new NotFoundException("Option with id "+ optionId + "  is not found"));
        menu.getOptionGroups().add(optionGroup);
        menuRepository.save(menu);
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Option successfully linked to menu.")
                .build();
    }


    private void menuBelongToManager(Long menuId){
        if(!menuRepository.existsByIdAndOwnerId(menuId, userService.getCurrentLoggedInUser().getId())){
            throw new ForbiddenException("This menu dont belong to current manager.");
        }
    }
    //WHERE category_id = ? AND (LOWER(name) LIKE ? OR LOWER(description) LIKE ?)
    private Specification<Menu> buildSpecification(Long categoryId, String search, Boolean myMenus){
        //Specification<Menu> is a lambda with root-> entity(Menu)
        //query->whole SQL query
        return (((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if(categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId)); //.get("fieldName")
            }
            if(myMenus) {
                User user = userService.getCurrentLoggedInUser();
                //make sure the user calling this has appropriate role
                userService.requireRole(user, RoleName.MANAGER);
                // We delegate to the new location
                Long restaurantId = userService.getRestaurantIdByCurrentLoggedUser().getId();
                predicates.add(criteriaBuilder.equal(root.get("category").get("restaurant").get("id"), restaurantId));
            }
            if(StringUtils.hasText(search)) {
                String searchTerm = "%" + search.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                searchTerm
                        ),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("description")),
                                searchTerm
                        )
                ));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }));
    }
}
