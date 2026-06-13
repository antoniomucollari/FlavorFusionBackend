package com.toni.FoodApp.payment.webhook.controller;

import com.toni.FoodApp.payment.dtos.request.PokWebhookPayload;
import com.toni.FoodApp.payment.webhook.service.PokPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PokPaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handlePokWebhook(@RequestBody PokWebhookPayload payload) {
        log.info("Received webhook payload: {}", payload);
        paymentService.processSuccessfulPayment(payload);
        return ResponseEntity.ok().build();
    }


}
