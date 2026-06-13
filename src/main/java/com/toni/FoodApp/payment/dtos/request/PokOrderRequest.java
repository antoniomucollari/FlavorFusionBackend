package com.toni.FoodApp.payment.dtos.request;

import com.toni.FoodApp.payment.dtos.response.PokProductDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PokOrderRequest {
    private long amount;
    private String currencyCode;
    private List<PokProductDTO> products;
    private boolean autoCapture;
    private long shippingCost;
    private String webhookUrl;
    private String redirectUrl;
    private String failRedirectUrl;
    private String deeplink;
    private int expiresAfterMinutes;
}