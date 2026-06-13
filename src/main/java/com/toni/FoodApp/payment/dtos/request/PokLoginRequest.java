package com.toni.FoodApp.payment.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PokLoginRequest {
    private String keyId;
    private String keySecret;
}