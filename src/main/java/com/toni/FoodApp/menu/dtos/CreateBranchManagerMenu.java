package com.toni.FoodApp.menu.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
@Builder
@Data
public class CreateBranchManagerMenu{
    private Long menuId;
    private String name;
    private BigDecimal price;
    private boolean isAvailable;
    private boolean isHighlighted;
}
