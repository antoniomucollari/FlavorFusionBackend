package com.toni.FoodApp.payment.webhook.service;


import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.payment.dtos.response.PokSdkOrder;
import com.toni.FoodApp.payment.dtos.request.PokWebhookPayload;

import java.math.BigDecimal;

public interface PokPaymentService {
    void processSuccessfulPayment(PokWebhookPayload payload);

    PokSdkOrder initiatePayment(Order order);

    PokSdkOrder refundPayment(String sdkOrderId, BigDecimal amount, String reason);
}
