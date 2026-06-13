package com.toni.FoodApp.order.entity;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.enums.payment.PaymentMethod;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.payment.entity.Payment;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "delivery_id")
    private User deliveryPerson;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<Payment> payment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;
    @Column(precision = 10, scale = 2)
    private BigDecimal serviceFee;
    @Column(precision = 10, scale = 2)
    private BigDecimal tipAmount;
    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;
    @Column(precision = 10, scale = 2)
    private BigDecimal deliveryPrice;

    private Long distanceInMeters;//todo change it to Integer important
    private Float estAvgDeliveryTimeInMinutes;
    private Float actualDeliveryTime;

    private LocalDateTime actualDeliveryDate;
    private LocalDateTime pickedUpAt;
    private LocalDateTime orderDate;
    @PrePersist
    protected void onCreate() {
        orderDate = LocalDateTime.now();
    }
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String deliveryNote;
    private Double latitude;
    private Double longitude;
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_branch_id")
    private RestaurantBranch branch;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    @Column(precision = 10, scale = 2)
    private BigDecimal  driverEarnings;
    private String reasonOfFailure;

    private String cartHash;
}
