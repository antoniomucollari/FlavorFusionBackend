package com.toni.FoodApp.cart.dtos;

import com.toni.FoodApp.deliverTo.dtos.DeliveryInfo;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderQuote {
    OrderSummary orderSummary;
    DeliveryInfo deliveryInfo;


}
