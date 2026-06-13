package com.toni.FoodApp.order.dtos;

import com.toni.FoodApp.payment.dtos.response.PaymentDTO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailsDto {
    private OrderDTO orderDTO;
    private LocalDateTime deliveryDate;
    private LocalDateTime pickedUpAt;
    private Integer reviewStars;
    private String reviewMessage;
    private LocalDateTime estDeliveryDate;
    private BigDecimal serviceFee;
    private String reasonOfFailure;
    private List<PaymentDTO> paymentDTO;
}
