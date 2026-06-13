package com.toni.FoodApp.category.service;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.aws.AWSS3Service;
import com.toni.FoodApp.category.dtos.RestaurantCategoryDTO;
import com.toni.FoodApp.category.dtos.RestaurantCategoryDTO;
import com.toni.FoodApp.category.entity.Category;
import com.toni.FoodApp.category.entity.RestaurantCategory;
import com.toni.FoodApp.category.repository.CategoryRepository;
import com.toni.FoodApp.category.repository.RestaurantCategoryRepository;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.order.repository.OrderItemRepository;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import com.toni.FoodApp.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantCategoryServiceImpl implements RestaurantCategoryService {

    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final RestaurantCategoryRepository restaurantCategoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final AWSS3Service awsS3Service;
    @Override
    public Response<RestaurantCategoryDTO> addCategory(RestaurantCategoryDTO categoryDTO) {
        log.info("Inside addCategory()");
        MultipartFile imageFile = categoryDTO.getImage();
        if(imageFile == null || imageFile.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        String imageUrl = awsS3Service.uploadImage(imageFile, "menus");;

        RestaurantCategory category = modelMapper.map(categoryDTO, RestaurantCategory.class);
        category.setRestaurantImageUrl(imageUrl);
        restaurantCategoryRepository.save(category);

        return Response.<RestaurantCategoryDTO>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Category Added successfully")
                .build();
    }

    @Override
    public Response<RestaurantCategoryDTO> updateCategory(RestaurantCategoryDTO categoryDTO) {
        log.info("Inside updateCategory()");

        RestaurantCategory restaurantCategory = restaurantCategoryRepository.findById(categoryDTO.getId())
                .orElseThrow(() -> new RuntimeException("Restaurant Category not found"));
        if(categoryDTO.getName() != null && !categoryDTO.getName().isEmpty())restaurantCategory.setName(categoryDTO.getName());
        MultipartFile imageFile = categoryDTO.getImage();
        if(imageFile == null && restaurantCategory.getRestaurantImageUrl() == null) {
            throw new BadRequestException("Image file is required");
        }

        String newImageUrl = awsS3Service.replaceImage(imageFile, restaurantCategory.getRestaurantImageUrl(), "menus");
        restaurantCategory.setRestaurantImageUrl(newImageUrl);
        restaurantCategoryRepository.save(restaurantCategory);
        return Response.<RestaurantCategoryDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Category Updated successfully")
                .build();
    }

    @Override
    public Response<RestaurantCategoryDTO> getCategoryById(Long id) {
        log.info("Inside getCategoryById()");
        Category category = categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
        RestaurantCategoryDTO dto = modelMapper.map(category, RestaurantCategoryDTO.class);
        return Response.<RestaurantCategoryDTO>builder()
                .statusCode(HttpStatus.CREATED.value())
                .data(dto)
                .message("Category retrieved successfully.")
                .build();
    }

    @Override
    public Response<?> deleteCategory(Long id) {
        if(!restaurantCategoryRepository.existsById(id)) throw new NotFoundException("Category not found");
        if(restaurantRepository.existsByCategoriesId(id))
            throw new IllegalStateException("Cannot delete category: it is still linked to one or more restaurants.");
        restaurantCategoryRepository.deleteById(id);
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Role deleted successfully")
                .build();
    }

    @Override
    public Response<List<RestaurantCategoryDTO>> getAllCategories() {
        List<RestaurantCategory> categories = restaurantCategoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<RestaurantCategoryDTO> categoryDTOS = modelMapper.map(categories, new org.modelmapper.TypeToken<List<RestaurantCategoryDTO>>(){}.getType()); //TypeToken
        return Response.<List<RestaurantCategoryDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Categories retrieved successfully.")
                .data(categoryDTOS)
                .build();
    }

    @Override
    public Response<?> updateRestaurantCategories(List<Long> categoryIds) {
        Set<Long> uniqueCategoryIds = new HashSet<>(categoryIds);


        if (uniqueCategoryIds.size() > 3 || uniqueCategoryIds.isEmpty()) {
            throw new BadRequestException("Cannot add more than 3 categories & can't be zero");
        }
        Restaurant restaurant = userService.getCurrentLoggedInUser().getRestaurant();

        Set<Long> currentCategoryIds = restaurant.getCategories().stream()
                .map(RestaurantCategory::getId)
                .collect(Collectors.toSet());

        if (currentCategoryIds.equals(uniqueCategoryIds)) {
            throw new BadRequestException("Categories are identical. No changes detected.");
        }

        List<RestaurantCategory> newCategories = restaurantCategoryRepository.findAllById(uniqueCategoryIds);

        if (newCategories.size() != uniqueCategoryIds.size()) {
            throw new NotFoundException("One or more categories were not found");
        }

        restaurant.getCategories().clear();
        restaurant.getCategories().addAll(newCategories);

        restaurantRepository.save(restaurant);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Categories updated successfully.")
                .build();
    }


}
