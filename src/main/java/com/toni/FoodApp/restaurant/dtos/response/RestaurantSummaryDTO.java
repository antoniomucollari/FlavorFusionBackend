package com.toni.FoodApp.restaurant.dtos.response;

import com.toni.FoodApp.category.dtos.RestaurantCategoryDTO;
import com.toni.FoodApp.restaurant.dtos.BranchSummaryDto;
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
public class RestaurantSummaryDTO {
        private Long id;
        private String name;
        private String description;
        private String coverImageUrl;
        private String profileImageUrl;
        private boolean isPromoted;
        private LocalDateTime createdAt;
    private List<RestaurantCategoryDTO> categories;
    private List<BranchSummaryDto> branches;
}