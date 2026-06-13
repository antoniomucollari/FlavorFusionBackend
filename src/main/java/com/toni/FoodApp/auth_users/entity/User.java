package com.toni.FoodApp.auth_users.entity;

import com.toni.FoodApp.cart.entity.Cart;
import com.toni.FoodApp.deliverTo.entity.DeliveryLocation;
import com.toni.FoodApp.enums.VerificationStatus;
import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.payment.entity.Payment;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.entity.Review;
import com.toni.FoodApp.role.entity.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "name is required")
    private String name;

    @Column(unique = true)
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String phoneNumber;
    private String profileUrl;
    private String address;
    private boolean isActive = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Review> reviews;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Cart> carts;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    @OneToOne
    @JoinColumn(name = "deliver_location_id", unique = true)
    private DeliveryLocation deliveryLocation;


    @OneToOne(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;


    @ManyToOne
    @JoinColumn(name = "last_selected_payment_method_id")
    private PaymentMethodEntity lastSelectedPaymentMethod;

    @OneToOne(mappedBy = "manager")
    private RestaurantBranch managedBranch;

    @NotNull
    private Boolean requirePasswordChange = false;

    private Long createdByCompany;

    @Builder.Default
    private int tokenVersion = 1;
}
