package com.toni.FoodApp.restaurant.entity;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.category.entity.RestaurantCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "restaurant")
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String coverImageUrl;
    private String profileImageUrl;
    private String phoneNumber;
    private boolean isPromoted;
    private Boolean isDeleted;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    @ToString.Exclude
    private User owner;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "restaurant_has_categories",
            joinColumns = @JoinColumn(name = "restaurant_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<RestaurantCategory> categories;


}
