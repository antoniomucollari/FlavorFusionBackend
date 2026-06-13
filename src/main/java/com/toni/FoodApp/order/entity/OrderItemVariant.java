package com.toni.FoodApp.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "order_item_variants")//important for snapshot
public class OrderItemVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    private String variantName;
    private BigDecimal priceCharged;

    //programming purpuse for OrderAgain feature.
    private Long originalOptionId;
    private Long originalVariantId;
}