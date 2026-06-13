package com.toni.FoodApp.menu.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
@Builder
@Data
public class BranchManagerMenu {
    private Long id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private boolean isAvailable;
    private boolean isHighlighted;
    private String categoryName;
}

