package com.toni.FoodApp.payment.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PokWebhookPayload {
    @JsonProperty("orderId")
    private String transactionId;
}