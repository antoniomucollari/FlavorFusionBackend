package com.toni.FoodApp.cart.services;

import com.toni.FoodApp.cart.dtos.CheckoutResponseDto;
import com.toni.FoodApp.cart.dtos.OrderQuote;
import com.toni.FoodApp.cart.entity.Cart;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.response.Response;

import java.math.BigDecimal;

public interface CheckoutService {

    Response<CheckoutResponseDto> generateCheckoutSummary(Long branchId);

    Response<CheckoutResponseDto> incrementItem(Long cartItemId);

    Response<CheckoutResponseDto> decrementItem(Long cartItemId);

    Response<CheckoutResponseDto> updateCartPaymentMethod(Long paymentMethodId,Long branchId);

    Response<CheckoutResponseDto> updateTip(BigDecimal amount, Long cartId);

    Response<CheckoutResponseDto> updateDeliveryNote(String note, Long cartId);

//    PaymentMethodEntity resolveSupportedPaymentMethod(Long branchId, PaymentMethodEntity selectedMethod);
    OrderQuote buildOrderQuote(Cart cart);
}
