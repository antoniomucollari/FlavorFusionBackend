package com.toni.FoodApp.cart.entity;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "carts")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL,orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CartItem> cartItems = new ArrayList<>();;

    @ManyToOne
    @JoinColumn(name = "restaurant_branch_id")
    private RestaurantBranch restaurantBranch;

    @Builder.Default
    private BigDecimal tipAmount =  BigDecimal.ZERO;

    private String deliveryNote;

    @ManyToOne
    @JoinColumn(name = "selected_payment_option_id")
    private PaymentMethodEntity selectedPaymentMethod;

    private String promoCode; //TODO
}
