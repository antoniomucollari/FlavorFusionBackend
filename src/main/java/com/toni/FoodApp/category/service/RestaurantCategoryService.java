package com.toni.FoodApp.category.service;

import com.toni.FoodApp.category.dtos.CategoryDTO;
import com.toni.FoodApp.category.dtos.RestaurantCategoryDTO;
import com.toni.FoodApp.category.entity.RestaurantCategory;
import com.toni.FoodApp.response.Response;

import java.util.List;

public interface RestaurantCategoryService {
    Response<RestaurantCategoryDTO> updateCategory(RestaurantCategoryDTO dto);

    Response<RestaurantCategoryDTO> addCategory(RestaurantCategoryDTO dto);

    Response<RestaurantCategoryDTO> getCategoryById(Long id);

    Response<?> deleteCategory(Long id);

    Response<List<RestaurantCategoryDTO>> getAllCategories();

    Response<?> updateRestaurantCategories(List<Long> categoryIds);
}
