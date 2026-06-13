package com.toni.FoodApp.payment.entity;


import com.toni.FoodApp.enums.payment.PaymentGateway;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.enums.payment.RefundStatus;
import com.toni.FoodApp.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.EnumSet;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Setter
@Getter
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PaymentGateway paymentGateway;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(updatable = false)
    private String transactionId;

    private LocalDateTime paymentDate;

    @Column(updatable = false)
    private Instant createdDate;

    private String paymentUrl;

    @Column(updatable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    private RefundStatus refundStatus;

}