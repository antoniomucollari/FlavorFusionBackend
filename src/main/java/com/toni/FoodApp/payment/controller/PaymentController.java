package com.toni.FoodApp.payment.controller;

import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.order.dtos.OrderDTO;
import com.toni.FoodApp.payment.dtos.response.PaymentDTO;
import com.toni.FoodApp.payment.dtos.request.BranchPaymentUpdateRequest;
import com.toni.FoodApp.payment.dtos.response.PaymentOptionDTO;
import com.toni.FoodApp.payment.dtos.response.PokSdkOrder;
import com.toni.FoodApp.payment.dtos.request.PaymentSearchCriteria;
import com.toni.FoodApp.payment.service.PaymentService;
import com.toni.FoodApp.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/payment"})
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<List<PaymentOptionDTO>>> getAvailablePaymentMethods() {
        return ResponseEntity.ok(paymentService.getAllMethods());
    }
    @PutMapping("/{paymentMethodId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<?>> editPaymentMethodName(
            @PathVariable Long paymentMethodId,
            @RequestBody PaymentOptionDTO request) {
        return ResponseEntity.ok(paymentService.editPaymentMethod(paymentMethodId, request));
    }

    @GetMapping("/restaurant-branch")
    @PreAuthorize("hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<Response<List<PaymentOptionDTO>>> getPaymentMethodsForBranch(){
        return ResponseEntity.ok(paymentService.getBranchPaymentConfiguration());
    }

    @PutMapping("/my-branch/update-methods")
    public ResponseEntity<Response<?>> updatePaymentMethods(@RequestBody BranchPaymentUpdateRequest request) {
        return ResponseEntity.ok(paymentService.updateBranchPaymentMethods(request));
    }

    @GetMapping("/check-payment-successful")
    public ResponseEntity<Response<PaymentStatusResponse>> checkPaymentSuccessful(@RequestParam String transactionId){
        return ResponseEntity.ok(paymentService.checkPaymentSuccessful(transactionId));
    }
    public record PaymentStatusResponse(String transactionId, PaymentStatus paymentStatus){
    }


    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<Response<PokSdkOrder>> refundPayment(
            @PathVariable Long paymentId,
            @RequestBody RefundReasonRequest request) { // DTO only contains the reason now

        return ResponseEntity.ok(paymentService.refundPayment(paymentId, request.reason()));
    }

    public record RefundReasonRequest(String reason) {}


    @GetMapping("/all")
    public ResponseEntity<Response<Page<PaymentDTO>>> getAll(
            PaymentSearchCriteria paymentSearchCriteria
    ){
        return ResponseEntity.ok(paymentService.getAllPayments(paymentSearchCriteria));
    }

    @PostMapping("/ask-for-refund/{orderId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<OrderDTO>> askForRefund(@PathVariable Long orderId){
        return ResponseEntity.ok(paymentService.askForRefund(orderId));
    }

    @PostMapping("/refund/{paymentId}")
    @PreAuthorize("hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<Response<PokSdkOrder>> refundPayment(@PathVariable Long paymentId, @RequestBody String reason){
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, reason));
    }

}
