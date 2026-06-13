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
public class BranchOptionVariantDto {
    private Long variantId;
    private String name;
    private BigDecimal globalPrice;

    // Branch specific fields
    private BigDecimal price;
    private boolean isAvailable;
    private boolean isOverwritten;
}
