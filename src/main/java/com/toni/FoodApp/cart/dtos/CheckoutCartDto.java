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
public class CheckoutCartDto {

    // --- Cart & Restaurant Info ---
    private Long id; // The Cart's ID
    private Long restaurantBranchId;
    private String restaurantBranchName;

    // --- Items ---
    private List<CartItemDTO> items;

    // --- Price Breakdown (Calculated by the Service) ---
    private BigDecimal subtotal;      // Sum of all items (quantity * price)
    private BigDecimal deliveryFee;   // Calculated by your pricing service
    private BigDecimal discount;      // Value of the promo code (e.g., $5.00)
    private BigDecimal total;         // The final price (subtotal + deliveryFee - discount)

    // --- Promotion ---
    private String promoCode;
}