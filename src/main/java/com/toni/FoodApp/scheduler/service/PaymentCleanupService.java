package com.toni.FoodApp.scheduler.service;

import com.toni.FoodApp.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCleanupService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public void processExpiredPayments() {
        int abandonedOrdersCount = paymentRepository.updateAbandonedOrders();
        int expiredPaymentsCount = paymentRepository.updateExpiredPayments();
        if (abandonedOrdersCount > 0 || expiredPaymentsCount > 0) {
            log.info("Cleanup successful: {} orders abandoned, {} payments expired.",
                    abandonedOrdersCount, expiredPaymentsCount);
        } else {
            log.debug("Cleanup ran: No pending payments found to expire.");
        }
    }
}