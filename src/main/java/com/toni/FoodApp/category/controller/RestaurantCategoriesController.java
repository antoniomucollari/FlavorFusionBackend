package com.toni.FoodApp.category.controller;

import com.toni.FoodApp.category.dtos.CategoryDTO;
import com.toni.FoodApp.category.dtos.RestaurantCategoryDTO;
import com.toni.FoodApp.category.entity.RestaurantCategory;
import com.toni.FoodApp.category.service.CategoryService;
import com.toni.FoodApp.category.service.RestaurantCategoryService;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant-categories")
@RequiredArgsConstructor //only works when there are final fields
public class RestaurantCategoriesController {
    private final CategoryService categoryService;
    private final RestaurantService restaurantService;
    private final RestaurantCategoryService restaurantCategoryService;


    @PostMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<RestaurantCategoryDTO>> addCategory(
            @ModelAttribute @Valid RestaurantCategoryDTO dto,
            @RequestParam(value = "imageFile") MultipartFile imageFile
    ){
        dto.setImage(imageFile);
        return ResponseEntity.ok(restaurantCategoryService.addCategory(dto));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<RestaurantCategoryDTO>> updateCategory(
            @ModelAttribute @Valid RestaurantCategoryDTO categoryDTO,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile
    ){
        categoryDTO.setImage(imageFile);
        return ResponseEntity.ok(restaurantCategoryService.updateCategory(categoryDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<RestaurantCategoryDTO>> getCategoryById(@PathVariable Long id){
        return ResponseEntity.ok(restaurantCategoryService.getCategoryById(id));
    }

    @GetMapping
    public ResponseEntity<Response<List<RestaurantCategoryDTO>>> getAllCategories(){
        return ResponseEntity.ok(restaurantCategoryService.getAllCategories());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<?>> deleteCategoryById(@PathVariable Long id){
        return ResponseEntity.ok(restaurantCategoryService.deleteCategory(id));
    }

    @PutMapping("/categories")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> updateRestaurantCategories(@RequestBody @NotNull List<Long> categoryIds) {
        return ResponseEntity.ok(restaurantCategoryService.updateRestaurantCategories(categoryIds));
    }
}
