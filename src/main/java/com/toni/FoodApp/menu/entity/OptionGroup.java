package com.toni.FoodApp.menu.entity;

import com.toni.FoodApp.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OptionGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int minSelection;
    private int maxSelection;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<OptionVariant> variants;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    @ToString.Exclude
    private Restaurant restaurant;
}