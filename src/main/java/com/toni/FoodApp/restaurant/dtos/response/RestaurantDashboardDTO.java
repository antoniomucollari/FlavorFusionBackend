package com.toni.FoodApp.restaurant.dtos.response;

import lombok.Builder;
import org.springframework.data.domain.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantDashboardDTO {

    private Page<RestaurantSummaryDTO> topRated;

    private Page<RestaurantSummaryDTO> trending;

    private Page<RestaurantSummaryDTO> fastest;

    private Page<RestaurantSummaryDTO> normal;
}