package com.toni.FoodApp.category.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestaurantCategoryDTO {

    private Long id;
    @NotBlank(message = "Name is required")
    private String name;
    private String restaurantImageUrl;
    private MultipartFile image;
}
