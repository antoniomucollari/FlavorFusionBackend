package com.toni.FoodApp.email_notification.services;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.email_notification.dtos.AccountSetupDto;
import com.toni.FoodApp.enums.RoleName;
import com.toni.FoodApp.order.dtos.OrderDTO;
import com.toni.FoodApp.response.Response;

public interface    NotificationService {
    void sendAccountSetupEmail(AccountSetupDto accountSetupDto, RoleName role);
    void sendOrderConfirmationEmail(User customer, OrderDTO orderDTO, Long branchId);
    void deactivateAccount(User user);
    void restoreAccount(User user);
}
