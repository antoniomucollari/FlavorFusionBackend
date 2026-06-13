package com.toni.FoodApp.menu.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.toni.FoodApp.category.dtos.SimpleCategoryDto;
import com.toni.FoodApp.restaurant.dtos.ReviewDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    @NotNull(message = "Category Id is required")
    private SimpleCategoryDto category;
    @JsonIgnore
    private MultipartFile imageFile;
}
