package com.toni.FoodApp.payment.repository;

import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Payment getPaymentByTransactionId(String paymentGatewayOrderId);

    Optional<Payment> findByTransactionId(String orderId);

    @Transactional
    @Modifying
    @Query(value = """
    UPDATE orders o
    SET payment_status = 'ABANDONED',
        order_status = 'FAILED'
    FROM payments p
    WHERE o.id = p.order_id
      AND o.order_status = 'INITIALIZED'
      AND o.order_status != 'COMPLETED'
      AND p.expires_at <= NOW()::timestamp
    """, nativeQuery = true)
    int updateAbandonedOrders();

    @Transactional
    @Modifying
    @Query(value = "UPDATE payments SET payment_status = 'EXPIRED' " +
            "WHERE payment_status = 'PENDING_PAYMENT' AND expires_at <= NOW()",
            nativeQuery = true)
    int updateExpiredPayments();
    List<Payment> findByIdAndPaymentStatus(Long id, PaymentStatus paymentStatus);
}
