package com.toni.FoodApp.restaurant.filterAndSpecification;

import lombok.Data;

@Data
public class BranchFilter {
    private Boolean includeDeleted = false;
    private Boolean hasManager;
}
