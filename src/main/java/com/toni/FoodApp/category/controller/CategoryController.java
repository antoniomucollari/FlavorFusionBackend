package com.toni.FoodApp.category.controller;

import com.toni.FoodApp.category.dtos.CategoryCreateDTO;
import com.toni.FoodApp.category.dtos.CategoryDTO;
import com.toni.FoodApp.category.dtos.SimpleCategoryDto;
import com.toni.FoodApp.category.service.CategoryService;
import com.toni.FoodApp.response.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor //only works when there are final fields
public class CategoryController {
    private final CategoryService categoryService;


    @PostMapping()
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ResponseEntity<Response<CategoryDTO>> addCategory(@RequestBody @Valid CategoryCreateDTO categoryDTO){
        return ResponseEntity.ok(categoryService.addCategory(categoryDTO));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> updateCategory(
            @RequestBody @Valid SimpleCategoryDto categoryDTO
    ){
        return ResponseEntity.ok(categoryService.updateCategory(categoryDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<CategoryDTO>> getCategoryById(@PathVariable Long id){
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @GetMapping
    public ResponseEntity<Response<List<SimpleCategoryDto>>> getAllCategories(){
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/manager")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<List<SimpleCategoryDto>>> getCategories(){
        return ResponseEntity.ok(categoryService.getAllCategoriesForRestaurantByUserId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> deleteCategoryById(@PathVariable Long id){
        return ResponseEntity.ok(categoryService.deleteCategory(id));
    }

    @PutMapping("/{id}/restore") // PUT is for updates
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> restoreCategoryById(@PathVariable Long id){
        return ResponseEntity.ok(categoryService.restoreCategory(id));
    }


}
