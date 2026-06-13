package com.toni.FoodApp.category.service;

import com.toni.FoodApp.category.dtos.CategoryCreateDTO;
import com.toni.FoodApp.category.dtos.CategoryDTO;
import com.toni.FoodApp.category.dtos.SimpleCategoryDto;
import com.toni.FoodApp.response.Response;

import java.util.List;

public interface CategoryService {
    Response<CategoryDTO> updateCategory(SimpleCategoryDto categoryDTO);

    Response<CategoryDTO> addCategory(CategoryCreateDTO categoryDTO);

    Response<CategoryDTO> getCategoryById(Long id);

    Response<?> deleteCategory(Long id);

    Response<List<SimpleCategoryDto>> getAllCategories();

    Response<List<SimpleCategoryDto>> getAllCategoriesForRestaurantByUserId();

    Response<?> restoreCategory(Long id);
}
