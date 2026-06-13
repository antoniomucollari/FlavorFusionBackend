package com.toni.FoodApp.cart.controller;

import com.toni.FoodApp.cart.dtos.CheckoutResponseDto;
import com.toni.FoodApp.cart.services.CheckoutService;
import com.toni.FoodApp.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor //only works when there are final fields
public class CheckoutController {

    private final CheckoutService checkoutService;


    @GetMapping("/{branchId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<CheckoutResponseDto>> getCheckoutPreview(@PathVariable Long branchId){
        return ResponseEntity.ok(checkoutService.generateCheckoutSummary(branchId));
    }

    @PostMapping("/increment/{cartItemId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<CheckoutResponseDto>> incrementItem(@PathVariable Long cartItemId){
        return  ResponseEntity.ok(checkoutService.incrementItem(cartItemId));
    }

    @PostMapping("/decrement/{cartItemId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<CheckoutResponseDto>> decrementItem(@PathVariable Long cartItemId){
        return  ResponseEntity.ok(checkoutService.decrementItem(cartItemId));
    }

    @PutMapping("/tip/{cartId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<CheckoutResponseDto>> updateTip(@PathVariable Long cartId, @RequestBody TipUpdateRequest request ){
        return  ResponseEntity.ok(checkoutService.updateTip(request.amount(), cartId));
    }

    @PutMapping("/delivery-note/{cartId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<CheckoutResponseDto>> updateDeliveryNote(@RequestBody DeliveryNoteRequest request, @PathVariable Long cartId ){
        return  ResponseEntity.ok(checkoutService.updateDeliveryNote(request.note,cartId));
    }

    @PutMapping("/payment-method")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<CheckoutResponseDto>> updatePaymentMethod(@RequestBody PaymentMethodRequest methodId ){
        return  ResponseEntity.ok(checkoutService.updateCartPaymentMethod(methodId.paymentMethodId, methodId.branchId));
    }


    public record TipUpdateRequest(BigDecimal amount) {}

    public record DeliveryNoteRequest(String note){}

    public record PaymentMethodRequest(Long paymentMethodId, Long branchId){}
}
