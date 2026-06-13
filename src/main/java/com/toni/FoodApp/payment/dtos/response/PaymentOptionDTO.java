package com.toni.FoodApp.payment.dtos.response;

import com.toni.FoodApp.enums.payment.PaymentMethod;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PaymentOptionDTO {
    private Long id;
    private String name;
    private PaymentMethod paymentMethod;

    @Builder.Default
    private boolean enabled = false;
}