package com.toni.FoodApp.cart.mapper;

import com.toni.FoodApp.cart.dtos.CartItemDTO;
import com.toni.FoodApp.cart.dtos.CheckoutResponseDto;
import com.toni.FoodApp.cart.dtos.OrderQuote;
import com.toni.FoodApp.cart.dtos.OrderSummary;
import com.toni.FoodApp.cart.entity.Cart;
import com.toni.FoodApp.cart.services.CartViewBuilderService;
import com.toni.FoodApp.payment.dtos.response.PaymentOptionDTO;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.payment.mapper.PaymentMethodMapper;
import com.toni.FoodApp.restaurant.mapper.RestaurantBranchMapper;
import com.toni.FoodApp.restaurant.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CheckoutMapper {
    private final CartMapper cartMapper;
    private final RestaurantBranchMapper restaurantBranchMapper;
    private final BranchService branchService;
    private final PaymentMethodMapper paymentMethodMapper;
    private final CartViewBuilderService cartViewBuilderService;

    public CheckoutResponseDto toDto(CheckoutSummaryContext context) {

        // Build the sub-DTOs by calling private helper methods
        OrderSummary orderSummary = context.quote.getOrderSummary();

        List<CartItemDTO> cartItemDTOs = context.cart().getCartItems().stream()
                .map(cartItem -> cartMapper.toDto(cartItem, cartViewBuilderService.getAvailableVariants(context.cart)))
                .toList();
        orderSummary.setCartItems(cartItemDTOs);
        CheckoutResponseDto.DeliveryDetailsDTO deliveryDetails = buildDeliveryDetails(context);
        CheckoutResponseDto.PaymentOptionsDTO paymentOptions = buildPaymentOptions(context);
        CheckoutResponseDto.TipOptionsDTO tipOptions = buildTipOptions(context);

        // Build the final DTO
        return CheckoutResponseDto.builder()
                .id(context.cart().getId())
                .orderSummary(orderSummary)
                .deliveryDetailsDTO(deliveryDetails)
                .paymentOptions(paymentOptions)
                .tipOptions(tipOptions)
                .branch(restaurantBranchMapper.toInfoDTO(context.branchId()))
                .build();
    }

    private CheckoutResponseDto.DeliveryDetailsDTO buildDeliveryDetails(CheckoutSummaryContext context) {
        return CheckoutResponseDto.DeliveryDetailsDTO.builder()
                .deliveryNote(context.cart().getDeliveryNote())
                .build();
    }

    private CheckoutResponseDto.PaymentOptionsDTO buildPaymentOptions(CheckoutSummaryContext context) {
        List<PaymentMethodEntity> availableMethodEntities = branchService.getAvailablePaymentMethods(context.branchId());
        List<PaymentOptionDTO> availableMethodDTOs = availableMethodEntities.stream()
                .map(method -> paymentMethodMapper.toDto(method))
                .toList();

        PaymentOptionDTO selectedMethodDTO = paymentMethodMapper.toDto(context.cart.getSelectedPaymentMethod());
        return CheckoutResponseDto.PaymentOptionsDTO.builder()
                .availableMethods(availableMethodDTOs)
                .selectedMethod(selectedMethodDTO)
                .build();
    }

    private CheckoutResponseDto.TipOptionsDTO buildTipOptions(CheckoutSummaryContext context) {
        return CheckoutResponseDto.TipOptionsDTO.builder()
                .selectedAmount(context.cart().getTipAmount())
                .suggestions(context.tipSuggestions())
                .build();
    }

    public record CheckoutSummaryContext(
            Cart cart,
            OrderQuote quote,
            List<BigDecimal> tipSuggestions,
            Long branchId
    ){}

}
