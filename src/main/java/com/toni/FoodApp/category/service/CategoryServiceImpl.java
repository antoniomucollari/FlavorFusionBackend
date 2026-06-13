package com.toni.FoodApp.category.service;

import com.toni.FoodApp.analytics.service.AnalyticsService;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.category.dtos.CategoryCreateDTO;
import com.toni.FoodApp.category.dtos.CategoryDTO;
import com.toni.FoodApp.category.dtos.SimpleCategoryDto;
import com.toni.FoodApp.category.entity.Category;
import com.toni.FoodApp.category.repository.CategoryRepository;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService{

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final AnalyticsService analyticsService;
    private final UserService userService;

    @Override
    public Response<CategoryDTO> addCategory(CategoryCreateDTO categoryDTO) {
        log.info("Inside addCategory()");
        Category category = modelMapper.map(categoryDTO, Category.class);
        category.setRestaurant(getRestaurantByUserId());
        categoryRepository.save(category);

        return Response.<CategoryDTO>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Category Added successfully")
                .build();
    }

    @Override
    public Response<CategoryDTO> updateCategory(SimpleCategoryDto categoryDTO) {
        log.info("Inside updateCategory()");

        Category category = categoryRepository.findById(categoryDTO.getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        if(categoryDTO.getName() != null && !categoryDTO.getName().isEmpty()) category.setName(categoryDTO.getName());
        categoryRepository.save(category);

        return Response.<CategoryDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Category Updated successfully")
                .build();
    }

    @Override
    public Response<CategoryDTO> getCategoryById(Long id) {
        log.info("Inside getCategoryById()");
        Category category = categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
        CategoryDTO dto = modelMapper.map(category, CategoryDTO.class);
        return Response.<CategoryDTO>builder()
                .statusCode(HttpStatus.CREATED.value())
                .data(dto)
                .message("Category retrieved successfully.")
                .build();
    }

    @Override
    public Response<?> deleteCategory(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(()-> new NotFoundException("Category not found"));
        category.setDeleted(true);
        categoryRepository.save(category);
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Role deleted successfully")
                .build();
    }

    @Override
    public Response<List<SimpleCategoryDto>> getAllCategories() { //id and name only
        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<SimpleCategoryDto> categoryDTOS = modelMapper.map(categories, new org.modelmapper.TypeToken<List<SimpleCategoryDto>>(){}.getType());
        return Response.<List<SimpleCategoryDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Categories retrieved successfully.")
                .data(categoryDTOS)
                .build();
    }

    @Override
    public Response<List<SimpleCategoryDto>> getAllCategoriesForRestaurantByUserId() {
        // We delegate to the new location
        Long restaurantId = userService.getRestaurantIdByCurrentLoggedUser().getId();
        List<Category> categories = categoryRepository.findByRestaurantIdOrderByIdAsc(restaurantId);
        List<SimpleCategoryDto> categoryDTOS = modelMapper.map(categories, new org.modelmapper.TypeToken<List<SimpleCategoryDto>>(){}.getType()); //TypeToken
        return Response.<List<SimpleCategoryDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Categories retrieved successfully.")
                .data(categoryDTOS)
                .build();
    }

    @Override
    public Response<?> restoreCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));

        if (!category.getDeleted()) {
            throw new BadRequestException("Category is already active.");
        }
        category.setDeleted(false);
        categoryRepository.save(category);
        return Response.<String>builder()
                .message("Category restored successfully")
                .build();
    }

    private Restaurant getRestaurantByUserId(){
        return userService.getCurrentLoggedInUser().getRestaurant();
    }



}
