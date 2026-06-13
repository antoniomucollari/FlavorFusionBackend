package com.toni.FoodApp.menu.entity;

import com.toni.FoodApp.category.entity.Category;
import com.toni.FoodApp.order.entity.OrderItem;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "menus")
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "menu")
    private List<OrderItem> orderItems;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "menu_option_groups",
            joinColumns = @JoinColumn(name = "menu_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    @OrderBy("id ASC")
    private List<OptionGroup> optionGroups;
}