package com.toni.FoodApp.order.service;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.enums.RoleName;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.ForbiddenException;
import com.toni.FoodApp.order.Validator.OrderStatusValidator;
import com.toni.FoodApp.order.dtos.OrderDTO;
import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.util.RoleSpecificationHelper;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderStatusProcessor {

    private final OrderStatusValidator orderStatusValidator;
    private final RoleSpecificationHelper roleSpecificationHelper;

    // This method handles all the business rules and mutates the Order entity
    public void processStatusUpdate(Order order, OrderDTO orderDTO, User user) {

        // 1. Authorize and Assign
        authorizeAndAssignUser(user, order, orderDTO);

        // 2. Apply Updates
        if (orderDTO.getOrderStatus() != null) {
            applyOrderStatusUpdate(order, orderDTO.getOrderStatus(), user);
        } else if (orderDTO.getPaymentStatus() != null) {
            applyPaymentStatusUpdate(order, orderDTO.getPaymentStatus());
        }
    }

    private void authorizeAndAssignUser(User user, Order order, OrderDTO orderDTO) {
        if (roleSpecificationHelper.isUserThisRole(user, RoleName.DELIVERY)) {
            orderStatusValidator.validateDeliveryUpdate(order, orderDTO);
            if (order.getDeliveryPerson() == null) {
                order.setDeliveryPerson(user);
            } else if (!order.getDeliveryPerson().getId().equals(user.getId())) {
                throw new ForbiddenException("You are not assigned to this delivery order.");
            }
        } else if (roleSpecificationHelper.isUserThisRole(user, RoleName.BRANCH_MANAGER)) {
            orderStatusValidator.validateBranchManagerOrderUpdate(order, user.getManagedBranch().getId());
        } else if (roleSpecificationHelper.isUserThisRole(user, RoleName.MANAGER)) {
            orderStatusValidator.validateRestaurantManagerOrderUpdate(order, user);
        } else if (roleSpecificationHelper.isUserThisRole(user, RoleName.CUSTOMER)) {
            orderStatusValidator.validateCustomerOrderUpdate(order, orderDTO, user.getId());
        }
    }

    private void applyOrderStatusUpdate(Order order, OrderStatus newStatus, User user) {
        if (order.getOrderStatus() == newStatus) {
            throw new BadRequestException("Order status is already " + newStatus + ". No update was performed.");
        }

        // Handle Delivery Timestamps
        if (roleSpecificationHelper.isUserThisRole(user, RoleName.DELIVERY)) {
            if (newStatus == OrderStatus.ON_THE_WAY && order.getActualDeliveryDate() == null) {
                order.setActualDeliveryDate(LocalDateTime.now());
            }
            if (newStatus == OrderStatus.DELIVERED && order.getActualDeliveryTime() == null) {
                float minutes = ChronoUnit.SECONDS.between(order.getActualDeliveryDate(), LocalDateTime.now()) / 60.0f;
                order.setActualDeliveryTime(minutes);
            }
        }

        // Handle Cancellations
        if (newStatus == OrderStatus.CANCELLED) {
            boolean isIncompletePayment = getIncompletePaymentStatuses().contains(order.getPaymentStatus());
            if (isIncompletePayment) {
                order.setPaymentStatus(PaymentStatus.CANCELED);
                order.getPayment().stream()
                        .filter(p -> getIncompletePaymentStatuses().contains(p.getPaymentStatus()))
                        .forEach(p -> p.setPaymentStatus(PaymentStatus.CANCELED));
            }
        }

        order.setOrderStatus(newStatus);
    }

    private void applyPaymentStatusUpdate(Order order, PaymentStatus newStatus) {
        if (order.getPaymentStatus() == newStatus) {
            throw new BadRequestException("Payment status is already " + newStatus + ". No update was performed.");
        }
        order.setPaymentStatus(newStatus);
    }

    // Helper method for incomplete payment statuses
    private List<PaymentStatus> getIncompletePaymentStatuses() {
        return List.of(PaymentStatus.PENDING, PaymentStatus.PENDING_PAYMENT, PaymentStatus.FAILED); // Adjust as needed
    }
}