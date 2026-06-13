package com.toni.FoodApp.menu.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.toni.FoodApp.restaurant.dtos.ReviewDTO;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class SimpleMenuDto {
    private Long id;
    private String name;
    @Nullable
    private String imageUrl;
}
