package com.toni.FoodApp.restaurant.repository;

import com.toni.FoodApp.restaurant.dtos.RestaurantFilterCriteria;
import com.toni.FoodApp.restaurant.dtos.RestaurantsDto;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    // --- 1. DEFINE RE-USABLE QUERY PARTS ---
    String BASE_SELECT = "SELECT r.* FROM restaurant r JOIN restaurant_locations b ON r.id = b.restaurant_id";

    String BASE_WHERE_DYNAMIC = """
             WHERE
              b.id IN (:branchIds)
              AND b.is_active = true
              AND b.is_closed = false
              AND b.manager_id is NOT NULL
              AND r.user_id is NOT NULL
              AND EXISTS (
                            SELECT 1 FROM restaurant_branch_payment_methods rbpm WHERE rbpm.restaurant_branch_id = b.id
                        )
              AND ST_DWithin(
                    b.location,
                    ST_MakePoint(:userLon, :userLat)::geography,
                    b.delivery_radius_in_km * 1000
                  )
              -- Dynamic Filters --
              AND ( CAST(:#{#criteria.isFeatured} AS BOOLEAN) IS NULL
                    OR CAST(:#{#criteria.isFeatured} AS BOOLEAN) = false
                    OR r.is_promoted = :#{#criteria.isFeatured} )
              AND ( CAST(:#{#criteria.minRating} AS DOUBLE PRECISION) IS NULL OR b.average_rating >= :#{#criteria.minRating} )
              AND ( CAST(:#{#criteria.maxPrepTime} AS INTEGER) IS NULL OR b.avg_prep_time_in_minutes < :#{#criteria.maxPrepTime} )
              AND ( CAST(:#{#criteria.isNew} AS BOOLEAN) IS NULL OR b.created_at > (NOW() - INTERVAL '30 days') )

              -- --- THIS IS THE CORRECTED LINE --- 
              AND ( CAST(:#{#criteria.search} AS VARCHAR) IS NULL\s
                    OR r.name ILIKE CONCAT('%', CAST(:#{#criteria.search} AS VARCHAR), '%') )
                   \s
              AND ( CAST(:#{#criteria.categoryId} AS INTEGER) IS NULL
                                  OR EXISTS (SELECT 1 FROM restaurant_has_categories rc  -- <--- Use your join table name
                                              WHERE rc.restaurant_id = r.id             -- This column is correct
                                              AND rc.category_id = :#{#criteria.categoryId}) )
              AND ( CAST(:#{#criteria.minOrderAmount} AS INTEGER) IS NULL
                                  OR b.min_order_amount <= :#{#criteria.minOrderAmount} )
           \s""";

    // The count query is now the same for all methods
    String BASE_COUNT = "SELECT COUNT(DISTINCT r.id) FROM restaurant r JOIN restaurant_locations b ON r.id = b.restaurant_id" + BASE_WHERE_DYNAMIC;
    String BASE_GROUP_BY = " GROUP BY r.id, r.is_promoted ";

    // --- 2. DEFINE YOUR DIFFERENT ORDER BY CLAUSES ---
    String ORDER_BY_DEFAULT = """
            ORDER BY
              r.is_promoted DESC,
              MIN(ST_Distance(b.location, ST_MakePoint(:userLon, :userLat)::geography)) ASC
            """;

    String ORDER_BY_RATING = """
            ORDER BY
              MAX(b.average_rating) DESC,
              MIN(ST_Distance(b.location, ST_MakePoint(:userLon, :userLat)::geography)) ASC
            """;

    String ORDER_BY_PREP_TIME = """
            ORDER BY
              MIN(b.avg_prep_time_in_minutes) ASC,
              MIN(ST_Distance(b.location, ST_MakePoint(:userLon, :userLat)::geography)) ASC
            """;

    String ORDER_BY_TRENDING = """
            ORDER BY
              MAX(b.daily_order_count) DESC,
              MIN(ST_Distance(b.location, ST_MakePoint(:userLon, :userLat)::geography)) ASC
            """;
    String ORDER_BY_ESTIMATED_TIME = """
            ORDER BY
              MIN(
                  b.avg_prep_time_in_minutes +\s
                  (ST_Distance(b.location, ST_MakePoint(:userLon, :userLat)::geography) / 250.0)
              ) ASC
           \s""";

    @Query(value = BASE_SELECT + BASE_WHERE_DYNAMIC + BASE_GROUP_BY + ORDER_BY_ESTIMATED_TIME,
            countQuery = BASE_COUNT, nativeQuery = true)
    Page<Restaurant> findDynamicAndRankedByTime(
            @Param("branchIds") List<Long> branchIds, @Param("userLon") double userLon, @Param("userLat") double userLat,
            @Param("criteria") RestaurantFilterCriteria criteria, Pageable pageable
    );
    @Query(value = BASE_SELECT + BASE_WHERE_DYNAMIC + BASE_GROUP_BY + ORDER_BY_TRENDING, countQuery = BASE_COUNT, nativeQuery = true)
    Page<Restaurant> findDynamicAndRankedByTrending(
            @Param("branchIds") List<Long> branchIds, @Param("userLon") double userLon, @Param("userLat") double userLat,
            @Param("criteria") RestaurantFilterCriteria criteria, Pageable pageable
    );
    @Modifying
    @Transactional
    @Query("UPDATE RestaurantBranch b SET b.dailyOrderCount = b.dailyOrderCount + 1 WHERE b.id = :branchId")
    void incrementDailyOrderCount(@Param("branchId") Long branchId);

    @Modifying
    @Transactional
    @Query("UPDATE RestaurantBranch b SET b.dailyOrderCount = b.dailyOrderCount - 1 WHERE b.id = :branchId AND b.dailyOrderCount > 0")
    void decrementDailyOrderCount(@Param("branchId") Long branchId);

    @Modifying
    @Transactional
        @Query("UPDATE RestaurantBranch b SET b.dailyOrderCount = 0")
    void resetAllDailyCounts();

    // Default Sort (by distance/promotion)
    @Query(value = BASE_SELECT + BASE_WHERE_DYNAMIC + BASE_GROUP_BY + ORDER_BY_DEFAULT, countQuery = BASE_COUNT, nativeQuery = true)
        Page<Restaurant> findDynamicAndRankedByDefault(
            @Param("branchIds") List<Long> branchIds, @Param("userLon") double userLon, @Param("userLat") double userLat,
            @Param("criteria") RestaurantFilterCriteria criteria, Pageable pageable
    );

    // Sort by Rating
    @Query(value = BASE_SELECT + BASE_WHERE_DYNAMIC + BASE_GROUP_BY + ORDER_BY_RATING, countQuery = BASE_COUNT, nativeQuery = true)
    Page<Restaurant> findDynamicAndRankedByRating(
            @Param("branchIds") List<Long> branchIds, @Param("userLon") double userLon, @Param("userLat") double userLat,
            @Param("criteria") RestaurantFilterCriteria criteria, Pageable pageable
    );

    // Sort by Prep Time
    @Query(value = BASE_SELECT + BASE_WHERE_DYNAMIC + BASE_GROUP_BY + ORDER_BY_PREP_TIME, countQuery = BASE_COUNT, nativeQuery = true)
    Page<Restaurant> findDynamicAndRankedByPrepTime(
            @Param("branchIds") List<Long> branchIds, @Param("userLon") double userLon, @Param("userLat") double userLat,
            @Param("criteria") RestaurantFilterCriteria criteria, Pageable pageable
    );


    @Query(value = """
        SELECT new com.toni.FoodApp.restaurant.dtos.RestaurantsDto(
            r.id, 
            r.name, 
            r.profileImageUrl, 
            r.isPromoted, 
            r.createdAt, 
            (SELECT COUNT(rb) FROM RestaurantBranch rb WHERE rb.restaurant.id = r.id),
            m.id, 
            m.name,
            r.isDeleted 
        )
        FROM Restaurant r
        JOIN r.owner m WHERE (:deleted IS NULL OR r.isDeleted = :deleted)
    """, countQuery = "SELECT COUNT(r) FROM Restaurant r WHERE (:deleted IS NULL OR r.isDeleted = :deleted)"
    )
    Page<RestaurantsDto> findRestaurantSummaries(Pageable pageable, Boolean deleted);


    boolean existsByCategoriesId(Long categoryId);
}