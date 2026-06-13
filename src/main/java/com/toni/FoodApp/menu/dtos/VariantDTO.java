package com.toni.FoodApp.menu.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantDTO {
    private Long id;
    private String name;
    private BigDecimal recommendedPrice;
    private boolean isDeleted = false;
    private Boolean isAvailable = true;
}
