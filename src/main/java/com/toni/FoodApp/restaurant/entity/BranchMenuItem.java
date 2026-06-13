package com.toni.FoodApp.restaurant.entity;

import com.toni.FoodApp.menu.entity.Menu;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "branch_menu_items")
public class BranchMenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private RestaurantBranch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    private BigDecimal price;

    @Builder.Default
    @Column(name = "is_highlighted")
    private boolean isHighlighted = false;

    @Builder.Default
    private boolean isAvailable = true;
}
