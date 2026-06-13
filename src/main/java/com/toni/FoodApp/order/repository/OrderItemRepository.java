package com.toni.FoodApp.order.repository;

import com.toni.FoodApp.analytics.dtos.PopularItemDto;
import com.toni.FoodApp.order.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END " +
            "FROM OrderItem oi " +
            "WHERE oi.order.id = :orderId AND oi.menu.id = :menuId" )
    boolean existsByOrderIdAndMenuId(
            @Param("orderId") Long orderId,
            @Param("menuId") Long menuId
    );

    @Query("""
    SELECT oi.menu.name, COUNT(oi) AS orderCount, oi.menu.imageUrl
    FROM OrderItem oi
    GROUP BY oi.menu.name, oi.menu.imageUrl
    ORDER BY orderCount DESC
""")
    List<Object[]> findMostPopularMenus(Pageable pageable);

    @Query("""
        SELECT new com.toni.FoodApp.analytics.dtos.PopularItemDto(
            oi.menu.name,\s
            COUNT(oi),\s
            oi.menu.imageUrl
        )
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.orderStatus = 'DELIVERED'\s
        AND (:restaurantId IS NULL OR o.branch.restaurant.id = :restaurantId)
        AND (:branchId IS NULL OR o.branch.id = :branchId)
        GROUP BY oi.menu.name, oi.menu.imageUrl
        ORDER BY COUNT(oi) DESC
   \s""")
    List<PopularItemDto> findMostPopularMenus(
            @Param("restaurantId") Long restaurantId,
            @Param("branchId") Long branchId,
            Pageable pageable
    );
}
