package com.toni.FoodApp.cart.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseCartDto {

    private Long id;
    private Long restaurantBranchId;
    private String restaurantBranchName;

    private List<CartItemDTO> items;

    private BigDecimal subtotal;

}