package com.toni.FoodApp.scheduler.controller;


import com.toni.FoodApp.analytics.service.DailyStatsService;
import com.toni.FoodApp.scheduler.service.PaymentCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/jobs")
@RequiredArgsConstructor
public class InternalJobController {

    private final DailyStatsService dailyStatsService;

    // Load a secret key from application.properties / environment variables
    @Value("${app.internal-job-secret}")
    private String expectedSecret;
    private final PaymentCleanupService cleanupService;

    @PostMapping("/daily-reset-order-count")//every day at 00:00
    public ResponseEntity<String> triggerOrderCountDailyReset(@RequestHeader("X-Internal-Secret") String providedSecret) {

        if (!expectedSecret.equals(providedSecret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }
        dailyStatsService.performDailyReset();

        return ResponseEntity.ok("Daily reset completed successfully.");
    }

    @PostMapping("/payment-cleanup") //30min is good
    public ResponseEntity<String> triggerPaymentReset(@RequestHeader("X-Internal-Secret") String providedSecret) {

        if (!expectedSecret.equals(providedSecret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }
        cleanupService.processExpiredPayments();

        return ResponseEntity.ok("payment clean up completed successfully.");
    }
}