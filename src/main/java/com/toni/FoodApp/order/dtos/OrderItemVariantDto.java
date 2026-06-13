package com.toni.FoodApp.order.dtos;

import com.toni.FoodApp.order.entity.OrderItem;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@RequiredArgsConstructor
public class OrderItemVariantDto {
    private Long id;
    private String variantName;
    private BigDecimal priceCharged;
}
