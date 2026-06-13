package com.toni.FoodApp.analytics.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerStatsDto {
    private long totalUniqueCustomers;
    private double percentageDifference;
    private long previousMonthCustomers;
    private long currentMonthCustomers;

}