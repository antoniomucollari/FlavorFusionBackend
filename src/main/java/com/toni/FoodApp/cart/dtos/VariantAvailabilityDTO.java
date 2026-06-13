package com.toni.FoodApp.cart.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

//TODO: change the field "price" name
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VariantAvailabilityDTO {
    private Long id;
    private Boolean isAvailable;
    private BigDecimal price;
    private String variantName;
    private Long cartItemId;
    public VariantAvailabilityDTO(Long id, Boolean isAvailable, BigDecimal price) {
        this.id = id;
        this.isAvailable = isAvailable;
        this.price = price;
    }
}
