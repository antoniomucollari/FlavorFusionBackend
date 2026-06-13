package com.toni.FoodApp.restaurant.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class SimpleRestaurantDto {
    private Long id;
    private String name;
    private String description;
    private String coverImageUrl;
    private String profileImageUrl;
    private boolean isPromoted;
    private LocalDateTime createdAt;

    private List<CategoryInfo> categories;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfo {
        private Long id;
        private String name;
    }

}
