package com.toni.FoodApp.analytics.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class BranchRevenueDto {
    private String branchName;
    private double revenue;
}
