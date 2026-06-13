package com.toni.FoodApp.cart.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Getter
@Setter
public class OrderSummary {

    private List<CartItemDTO> cartItems;

    private BigDecimal subtotal;

    private BigDecimal deliveryFee;

    private BigDecimal serviceFee;

    private BigDecimal tipAmount;

    private BigDecimal totalAmount;
}