package com.toni.FoodApp.cart.entity;

import com.toni.FoodApp.restaurant.entity.BranchMenuItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_menu_item_id")
    private BranchMenuItem branchMenuItem;

    private int quantity;

    @Column(nullable = false)
    private BigDecimal pricePerUnit;
    private BigDecimal subTotal;

    @OneToMany(mappedBy = "cartItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItemVariant> cartItemVariants = new ArrayList<>();

}
