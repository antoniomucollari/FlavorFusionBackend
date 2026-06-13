package com.toni.FoodApp.menu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OptionVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private BigDecimal recommendedPrice;
    @ManyToOne
    @JoinColumn(name = "group_id")
    private OptionGroup group;
    @Column(nullable = false)
    private boolean isDeleted = false;
}