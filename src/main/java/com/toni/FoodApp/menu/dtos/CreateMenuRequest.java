package com.toni.FoodApp.menu.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.toni.FoodApp.restaurant.dtos.ReviewDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateMenuRequest {
    private Long id;
    private String name;
    private String description;
    private MultipartFile image;
    @NotNull(message = "Category Id is required")
    private Long categoryId;
    @JsonIgnore
    private MultipartFile imageFile;
    private List<ReviewDTO> reviews;
}
