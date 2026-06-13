package com.toni.FoodApp.order.repository;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> , JpaSpecificationExecutor<Order> {

    Page<Order> findByUserOrderByOrderDateDesc(User user, Pageable pageable);

//    @Query("SELECT COUNT(DISTINCT o.user.id)FROM Order o")
//    Long countDistinctUsers();


    //DEPRECATED
    @Query(value = "SELECT " +
            "COUNT(DISTINCT o.user_id) as totalUnique, " +
            "COUNT(DISTINCT CASE WHEN o.order_created_date >= :currentStart AND o.order_created_date < :currentEnd THEN o.user_id END) as currentMtd, " +
            "COUNT(DISTINCT CASE WHEN o.order_created_date >= :prevStart AND o.order_created_date < :prevEnd THEN o.user_id END) as previousMtd " +
            "FROM orders o " + // Ensure this matches your DB table name
            "WHERE (:restaurantId IS NULL OR o.restaurant_id = :restaurantId)",
            nativeQuery = true)
    CustomerStatsView getCustomerStats(
            @Param("currentStart") LocalDateTime currentStart,
            @Param("currentEnd") LocalDateTime currentEnd,
            @Param("prevStart") LocalDateTime prevStart,
            @Param("prevEnd") LocalDateTime prevEnd,
            @Param("restaurantId") Long restaurantId
    );
    Page<Order> findByUserAndPaymentStatusOrderByOrderDateDesc(User customer, PaymentStatus paymentStatus, Pageable pageable);

    Page<Order> findByUserAndOrderStatusOrderByOrderDateDesc(User user, OrderStatus orderStatus,Pageable pageable);

    Page<Order> findByUserAndOrderStatusAndPaymentStatusOrderByOrderDateDesc(User user, OrderStatus orderStatus, PaymentStatus paymentStatus,Pageable pageable);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.orderStatus = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") OrderStatus status);

    @Query("SELECT SUM(o.totalAmount) " +
            "FROM Order o " +
            "WHERE o.orderStatus = :status " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountByStatusPreviousMonth(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    @Query("SELECT o.orderStatus, COUNT(o) FROM Order o GROUP BY o.orderStatus")
    List<Object[]> countOrdersByStatus();
    
    //DEPRECATED
    @Query("SELECT EXTRACT(MONTH FROM o.orderDate), SUM(o.totalAmount) FROM Order o WHERE EXTRACT(YEAR FROM o.orderDate) = :year AND o.orderStatus = :status GROUP BY EXTRACT(MONTH FROM o.orderDate)")
    List<Object[]> getMonthlyRevenueByYear(@Param("year") int year, @Param("status") OrderStatus status);
    //DEPRECATED
    @Query("SELECT EXTRACT(DAY FROM o.orderDate), SUM(o.totalAmount) FROM Order o WHERE EXTRACT(YEAR FROM o.orderDate) = :year AND EXTRACT(MONTH FROM o.orderDate) = :month AND o.orderStatus = :status GROUP BY EXTRACT(DAY FROM o.orderDate)")
    List<Object[]> getDailyRevenueForMonth(@Param("year") int year, @Param("month") int month, @Param("status") OrderStatus status);
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE o.orderStatus = :status AND o.orderDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountByStatusAndDateRange(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(DISTINCT o.user.id) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    long countDistinctUsersByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p.order FROM Payment p WHERE p.transactionId = :transactionId")
    Optional<Order> findByPaymentTransactionId(String transactionId);

    Optional<Order> findFirstByUserIdAndBranchIdAndPaymentStatusOrderByIdDesc(Long userId, Long branchId, PaymentStatus paymentStatus);

    public interface CustomerStatsView {
        Long getTotalUnique();
        Long getCurrentMtd();
        Long getPreviousMtd();
    }

    //=============================================new ========================================================
    // 1. Monthly Revenue Query

    @Query("""
        SELECT EXTRACT(MONTH FROM o.orderDate), SUM(o.totalAmount)\s
        FROM Order o\s
        WHERE EXTRACT(YEAR FROM o.orderDate) = :year\s
        AND o.orderStatus = 'DELIVERED'
        AND (:restaurantId IS NULL OR o.branch.restaurant.id = :restaurantId)
        AND (:branchId IS NULL OR o.branch.id = :branchId)
        GROUP BY EXTRACT(MONTH FROM o.orderDate)
   \s""")
    List<Object[]> getMonthlyRevenueByYear(
            @Param("year") int year,
            @Param("restaurantId") Long restaurantId,
            @Param("branchId") Long branchId
    );

    // 2. Daily Revenue Query
    @Query("""
        SELECT EXTRACT(DAY FROM o.orderDate), SUM(o.totalAmount)\s
        FROM Order o\s
        WHERE EXTRACT(YEAR FROM o.orderDate) = :year\s
        AND EXTRACT(MONTH FROM o.orderDate) = :month\s
        AND o.orderStatus = 'DELIVERED'
        AND (:restaurantId IS NULL OR o.branch.restaurant.id = :restaurantId)
        AND (:branchId IS NULL OR o.branch.id = :branchId)
        GROUP BY EXTRACT(DAY FROM o.orderDate)
   \s""")
    List<Object[]> getDailyRevenueForMonth(
            @Param("year") int year,
            @Param("month") int month,
            @Param("restaurantId") Long restaurantId,
            @Param("branchId") Long branchId
    );

    @Query("select o from Order o where o.id = :id and o.user.id = :userId")
    Optional<Order> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Page<Order> findByDeliveryPersonIsNullAndOrderStatusIn(
            Collection<OrderStatus> statuses, Pageable pageable
    );

    @Query("select COUNT(o) from Order o where o.deliveryPerson.id = :userId and (o.orderStatus = 'CONFIRMED' or o.orderStatus = 'PREPARING') ")
    Long countByDeliveryPersonAndStillPreparing(@Param("userId") Long id);

    @Query("SELECT MONTH(o.orderDate), SUM(o.driverEarnings) " +
            "FROM Order o " +
            "WHERE YEAR(o.orderDate) = :year " +
            "AND o.deliveryPerson.id = :deliveryId " +
            "AND o.orderStatus = 'DELIVERED' " +
            "GROUP BY MONTH(o.orderDate)")
    List<Object[]> getMonthlyEarningsByYear(@Param("year") int year, @Param("deliveryId") Long deliveryId);
}
