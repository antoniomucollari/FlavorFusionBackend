package com.toni.FoodApp.restaurant.entity;

import com.toni.FoodApp.menu.entity.OptionVariant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "branch_id", "variant_id" }) }) //NOT allowed duplicated, db buissnes rule Blloku + Truffle Mayo (twice)
public class BranchOptionConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private RestaurantBranch branch;
    @ManyToOne
    private OptionVariant variant;    // Link to "Salca tartuf"

    private BigDecimal priceOverride; // e.g., 80.00 (Overrides the default 50.00)

    private Boolean isAvailable;      // e.g., false (If Blloku runs out of stock)
}