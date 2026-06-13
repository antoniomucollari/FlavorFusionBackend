package com.toni.FoodApp.cart.dtos;

public class CheckoutResponse {
    private Long orderId;
    private String paymentUrl;
    private String paymentGatewayOrderId;
    private String paymentStatus;
}
