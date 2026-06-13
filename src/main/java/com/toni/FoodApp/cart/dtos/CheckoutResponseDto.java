package com.toni.FoodApp.cart.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.toni.FoodApp.payment.dtos.response.PaymentOptionDTO;
import com.toni.FoodApp.restaurant.dtos.RestaurantBranchInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutResponseDto {
    private Long id;

    private List<CartItemDTO> cartItems;

    private RestaurantBranchInfoDTO branch;

    private PaymentOptionsDTO paymentOptions;

    private DeliveryDetailsDTO deliveryDetailsDTO;

    private OrderSummary orderSummary;

    private TipOptionsDTO tipOptions;

    @Builder
    @Data
    public static class TipOptionsDTO {
        private List<BigDecimal> suggestions;
        private BigDecimal selectedAmount;
    }

    @Builder
    @Data
    public static class PaymentOptionsDTO {
        private List<PaymentOptionDTO> availableMethods;
        private PaymentOptionDTO selectedMethod;
    }

    @Builder
    @Data
    public static class DeliveryDetailsDTO {
        private String deliveryNote;
    }
}

