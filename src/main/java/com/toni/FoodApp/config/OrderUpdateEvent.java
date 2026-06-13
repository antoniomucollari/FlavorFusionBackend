package com.toni.FoodApp.config;

import com.toni.FoodApp.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderUpdateEvent {
    private final Order order;
    private final Order previousState; // Optional: helps decide if we need to remove an order from a list
}