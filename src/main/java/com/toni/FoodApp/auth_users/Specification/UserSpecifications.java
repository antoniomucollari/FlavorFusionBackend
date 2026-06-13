package com.toni.FoodApp.auth_users.Specification;

import com.toni.FoodApp.auth_users.entity.User;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class UserSpecifications {

    public static Specification<User> filterBranchManagers(Long restaurantId, Long id, String name, String branchName, Boolean isAssigned) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("createdByCompany"), restaurantId));

            Join<Object, Object> rolesJoin = root.join("roles");
            predicates.add(criteriaBuilder.equal(rolesJoin.get("name"), "BRANCH_MANAGER"));

            Join<Object, Object> branchJoin = root.join("managedBranch", JoinType.LEFT);

            Predicate branchIsNull = criteriaBuilder.isNull(root.get("managedBranch"));
            Predicate branchBelongsToRestaurant = criteriaBuilder.equal(branchJoin.get("restaurant").get("id"), restaurantId);
            predicates.add(criteriaBuilder.or(branchIsNull, branchBelongsToRestaurant));

            if (isAssigned != null) {
                if (isAssigned) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("managedBranch")));
                } else {
                    predicates.add(criteriaBuilder.isNull(root.get("managedBranch")));
                }
            }

            if (id != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), id));
            }

            if (name != null && !name.trim().isEmpty()) {
                String searchString = "%" + name.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchString),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchString)
                ));
            }

            if (branchName != null && !branchName.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(branchJoin.get("address")),
                        "%" + branchName.toLowerCase() + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}