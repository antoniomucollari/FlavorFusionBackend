package com.toni.FoodApp.payment.mapper;

import com.toni.FoodApp.cart.dtos.VariantAvailabilityDTO;
import com.toni.FoodApp.order.entity.OrderItem;
import com.toni.FoodApp.order.entity.OrderItemVariant;
import com.toni.FoodApp.payment.dtos.response.PaymentDTO;
import com.toni.FoodApp.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMapper {

    public PaymentDTO mapToPaymentDto(Payment payment){
        return PaymentDTO.builder()
                .id(payment.getId())
                .paymentDate(payment.getPaymentDate())
                .createdDate(payment.getCreatedDate())
                .expiresAt(payment.getExpiresAt())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .paymentStatus(payment.getPaymentStatus())
                .transactionId(payment.getTransactionId())
                .paymentGateway(payment.getPaymentGateway())
                .refundStatus(payment.getRefundStatus())
                .paymentGateway(payment.getPaymentGateway())
                .paymentUrl(payment.getPaymentUrl())
                .build();
    }
}
