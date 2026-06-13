package com.toni.FoodApp.payment.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BranchPaymentUpdateRequest {
    private Set<Long> paymentMethodIds;
}