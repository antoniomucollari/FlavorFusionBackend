package com.toni.FoodApp.auth_users.services;

import com.toni.FoodApp.auth_users.dtos.ChangePasswordRequest;
import com.toni.FoodApp.auth_users.dtos.UserDTO;
import com.toni.FoodApp.auth_users.dtos.UserFilterRequest;
import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.enums.RoleName;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.entity.Restaurant;

import java.util.List;

public interface UserService {
    User getCurrentLoggedInUser();

    Response<List<UserDTO>> getAllUsers(UserFilterRequest filter);

    Response<List<UserDTO>> getAllBranchManagers(Long id, String name, String branchName,Boolean isAssigned);

    Response<UserDTO> getOwnAccountDetails(User user);

    Response<?> updateOwnAccount(UserDTO userDTO);

    Response<?> changeRole(Long userId, RoleName newRole);
    Response<?> deactivateAccount(Long id);

    void updateSelectedPaymentMethodAndSaveTheUser(User user, PaymentMethodEntity method);
    void requireRole(User user, RoleName roleName);

    Response<?> deactivateOwnAccountEmail();
    Restaurant getRestaurantIdByCurrentLoggedUser();

    Response<?> changePassword(ChangePasswordRequest changePasswordRequest);

    Response<?> deactivateBranchManager(Long id);

    Response<?> restoreBranchManager(Long id);

    Response<?> restoreUsers(Long id);
    User getCurrentLoggedInUserOrNull();
}
