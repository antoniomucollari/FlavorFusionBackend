package com.toni.FoodApp.restaurant.entity;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import io.micrometer.common.lang.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// ... other imports ...

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "restaurant_locations")
@EntityListeners(RestaurantBranchListener.class)
public class    RestaurantBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String address;
    @Column(columnDefinition = "geography(Point,4326)")
    private Point location;

    private String phoneNumber;
    private boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL)
    private List<Order> orders;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OpeningHour> openingHours = new ArrayList<>();

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BranchMenuItem> availableItems = new ArrayList<>();

    @Column(name = "delivery_radius_in_km")
    @Max(8)
    private Double deliveryRadiusInKm;

    private boolean isClosed = true;

    private Integer minOrderAmount;
    private Integer avgPrepTimeInMinutes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Column(name = "average_rating", precision = 3, scale = 2) //max 9.99 so total 3 digits
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @ManyToMany(fetch = FetchType.LAZY) // LAZY is critical for performance
    @JoinTable(
            name = "restaurant_branch_payment_methods",
            joinColumns = @JoinColumn(name = "restaurant_branch_id"),
            inverseJoinColumns = @JoinColumn(name = "payment_   method_id")
    )
    private Set<PaymentMethodEntity> paymentMethods = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "manager_id")
    private User manager;

    @Nullable
    private Boolean deleted = false;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int dailyOrderCount = 0;
}