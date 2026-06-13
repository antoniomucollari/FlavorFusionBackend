package com.toni.FoodApp.restaurant.dtos;

import com.toni.FoodApp.category.dtos.RestaurantCategoryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantsDto {
        private Long id;
        private String name;
        private String profileImageUrl;
        private boolean isPromoted;
        private LocalDateTime createdAt;
        private Long numberOfBranches;
        private Long managerId;
        private String managerFullName;
        private Boolean isDeleted;
}