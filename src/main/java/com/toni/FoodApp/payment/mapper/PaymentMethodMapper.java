package com.toni.FoodApp.payment.mapper;

import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.order.entity.OrderItem;
import com.toni.FoodApp.payment.dtos.response.PaymentOptionDTO;
import com.toni.FoodApp.payment.dtos.request.PokOrderRequest;
import com.toni.FoodApp.payment.dtos.response.PokProductDTO;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaymentMethodMapper {
    @Value("${pok.webhookUrl}")
    private String webhookUrl;
    @Value("${pok.redirectUrl}")
    private String redirectUrl;
    @Value("${pok.failRedirectUrl}")
    private String failRedirectUrl;
    @Value("${pok.expiersAfterMinutes}")
    private int expiresAfterMinutes;
    public PaymentOptionDTO toDto(PaymentMethodEntity entity) {
        if (entity == null) {
            return null;
        }
        return PaymentOptionDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .paymentMethod(entity.getPaymentMethod())
                .build();
    }

    public PokOrderRequest buildPokOrderRequest(Order order){
        List<PokProductDTO> pokProducts = mapToPokProducts(order.getOrderItems());
        return PokOrderRequest.builder()
                .amount(order.getSubtotal().longValue())
                .currencyCode("ALL")
                .products(pokProducts)
                .autoCapture(true)
                .shippingCost(order.getDeliveryPrice().longValue())
                .webhookUrl(webhookUrl)
                .redirectUrl(redirectUrl)
                .failRedirectUrl(failRedirectUrl)
                .expiresAfterMinutes(expiresAfterMinutes)
                .build();
    }


    private List<PokProductDTO> mapToPokProducts(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(item -> new PokProductDTO(
                        item.getItemName(),
                        item.getQuantity(),
                        // usage of longValue() suggests the API might need cents/integers
                        item.getPricePerUnit().multiply(BigDecimal.valueOf(1)).longValue()
                ))
                .collect(Collectors.toList());
    }
}