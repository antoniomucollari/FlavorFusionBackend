package com.toni.FoodApp.util;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.enums.RoleName;
import com.toni.FoodApp.exceptions.ForbiddenException;
import com.toni.FoodApp.order.entity.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Function;
@Component
public class RoleSpecificationHelper {
    public <T> Specification<T> applyRoleBasedSecurity(
            User currentUser,
            Specification<T> baseSpec,
            Function<Long, Specification<T>> restaurantSpecProvider,
            Function<Long, Specification<T>> branchSpecProvider,
            Function<Long, Specification<T>> deliverySpecProvider) {

        boolean isAdmin = isUserThisRole(currentUser, RoleName.ADMIN);
        boolean isRestaurantManager = isUserThisRole(currentUser, RoleName.MANAGER);
        boolean isBranchManager = isUserThisRole(currentUser, RoleName.BRANCH_MANAGER);
        boolean isDelivery = isUserThisRole(currentUser, RoleName.DELIVERY);

        if (isAdmin) {
            // Admins see everything that matches the base filters
            return baseSpec;
        } else if (isRestaurantManager) {
            if (currentUser.getRestaurant() == null) throw new ForbiddenException("No restaurant assigned.");
            return baseSpec.and(restaurantSpecProvider.apply(currentUser.getRestaurant().getId()));
        } else if (isBranchManager) {
            if (currentUser.getManagedBranch().getId() == null) throw new ForbiddenException("No branch assigned.");
            return baseSpec.and(branchSpecProvider.apply(currentUser.getManagedBranch().getId()));
        } else if (isDelivery) {
            return baseSpec.and(deliverySpecProvider.apply(currentUser.getId()));
        } else {
            throw new ForbiddenException("You are not authorized to view this resource.");
        }
    }

    public boolean isUserThisRole(User user, RoleName roleName){
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }
    public void validateOrderBelongToUser(Long userId, Order order){
        if(! Objects.equals(order.getUser().getId(), userId)){
            throw new ForbiddenException("You are not authorized to view this resource.");
        }
    }


}
