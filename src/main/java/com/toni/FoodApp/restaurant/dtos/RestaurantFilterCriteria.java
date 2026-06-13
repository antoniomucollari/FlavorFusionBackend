package com.toni.FoodApp.restaurant.dtos;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RestaurantFilterCriteria {
    // We use wrapper types (Boolean, Double) so they can be 'null'

    // Example filters:
    private Boolean isFeatured;
    private Double minRating;
    private Integer maxPrepTime;
    private Boolean isNew;
    private String search;
    private Integer categoryId;
    private Integer minOrderAmount;


    public boolean isSearchActive() {
        return search != null && !search.trim().isEmpty();
    }

    public String toCacheKey() {
        return String.join(":",
                isFeatured != null ? "f=" + isFeatured : "f=any",
                minRating != null ? "r=" + minRating : "r=any",
                maxPrepTime != null ? "pt=" + maxPrepTime : "pt=any",
                isNew != null ? "new=" + isNew : "new=any",
                categoryId != null ? "cat=" + categoryId : "cat=any",
                minOrderAmount != null ? "min=" + minOrderAmount : "min=any"
        );
    }
}