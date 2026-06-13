package com.toni.FoodApp.payment.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.toni.FoodApp.enums.payment.PaymentGateway;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.enums.payment.RefundStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class PaymentDTO {
    private Long id;
    private Long orderId;

    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private String transactionId;
    private PaymentGateway paymentGateway;

//    private String failureReason;

    private LocalDateTime paymentDate;

    private RefundStatus refundStatus;

    private Instant createdDate;
    private Instant expiresAt;
    private String paymentUrl;

}