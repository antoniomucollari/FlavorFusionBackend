package com.toni.FoodApp.menu.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BranchConfigUpdateRequest {
    @NotNull
    private Long variantId;

    @Min(0)
    private BigDecimal price;

    private Boolean isAvailable = true;
}