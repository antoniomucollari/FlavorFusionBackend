package com.toni.FoodApp.restaurant.filterAndSpecification;

import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class BranchSpecifications {

    public static Specification<RestaurantBranch> forRestaurant(
            Long restaurantId, BranchFilter filter
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(
                    root.get("restaurant").get("id"), restaurantId
            ));

            if (!Boolean.TRUE.equals(filter.getIncludeDeleted())) {
                predicates.add(cb.isFalse(root.get("deleted")));
            }

            if (filter.getHasManager() != null) {
                predicates.add(
                        filter.getHasManager()
                                ? cb.isNotNull(root.get("manager"))
                                : cb.isNull(root.get("manager"))
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
