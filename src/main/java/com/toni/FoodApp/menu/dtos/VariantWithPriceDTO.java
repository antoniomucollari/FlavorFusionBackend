package com.toni.FoodApp.menu.dtos;

import com.toni.FoodApp.menu.entity.OptionVariant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VariantWithPriceDTO {
    private OptionVariant variant;
    private BigDecimal effectivePrice;
}