package com.toni.FoodApp.deliverTo.dtos;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DeliveryInfo {
    private final BigDecimal deliveryFee;
    private final String deliveryTime;
    private final double distanceInKm;
    private final long distanceInMeters;
    private final long durationInSeconds;
    private final boolean isDeliverable;
}