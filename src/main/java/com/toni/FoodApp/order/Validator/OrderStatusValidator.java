package com.toni.FoodApp.order.Validator;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.enums.payment.PaymentMethod;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.ForbiddenException;
import com.toni.FoodApp.order.dtos.OrderDTO;
import com.toni.FoodApp.order.entity.Order;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Objects;

@Component
public class OrderStatusValidator {


    public void validateDeliveryUpdate(Order order, OrderDTO orderDTO) {
        if (orderDTO.getOrderStatus() != null) {
            validateOrderStatusDelivery(order, orderDTO.getOrderStatus());
        } else if (orderDTO.getPaymentStatus() != null && order.getPaymentMethod() != PaymentMethod.POK) {
            validatePaymentStatusDelivery(orderDTO.getPaymentStatus());
        } else {
            throw new BadRequestException("No valid status update provided or you dont have authority to change the status.");
        }
    }
    private void validateOrderStatusDelivery(Order order, OrderStatus newStatus) {
        if (!EnumSet.of(OrderStatus.DELIVERED, OrderStatus.FAILED, OrderStatus.ON_THE_WAY).contains(newStatus)) {
            throw new ForbiddenException("Status " + newStatus + " cannot be set by delivery person.");
        }

        boolean validTransition = switch (newStatus) {
            case ON_THE_WAY -> order.getOrderStatus() == OrderStatus.READY_FOR_PICKUP;
            case DELIVERED, FAILED -> order.getOrderStatus() == OrderStatus.ON_THE_WAY;
            default -> false;
        };

        if (!validTransition) {
            throw new BadRequestException("Invalid transition from " + order.getOrderStatus() + " to " + newStatus);
        }
    }
    private void validatePaymentStatusDelivery(PaymentStatus newPaymentStatus) {
        if (!EnumSet.of(PaymentStatus.COMPLETED, PaymentStatus.FAILED).contains(newPaymentStatus)) {
            throw new ForbiddenException("Payment status " + newPaymentStatus + " cannot be set by delivery person.");
        }
    }
//--------------------finished with delivery---------------------------------------

    public void validateCustomerOrderUpdate(Order order, OrderDTO orderDTO, Long userId) {
        if(orderDTO.getOrderStatus() != OrderStatus.CANCELLED) throw new ForbiddenException("Customer only can set the order to canceled.");
        if(! Objects.equals(order.getUser().getId(), userId)) throw new ForbiddenException("You cant cancel the order of other customers!");
        if(orderDTO.getPaymentStatus() != null) throw new ForbiddenException("You can't change the payment status.");
    }

    public void validateBranchManagerOrderUpdate(Order order, Long branchId) {
        if (branchId == null) {
            throw new ForbiddenException("Current manager does not manage any branches");
        }
        if (order.getBranch() == null || !order.getBranch().getId().equals(branchId)) {
            throw new ForbiddenException("You are not authorized to update orders outside your branch.");
        }
    }

    public void validateRestaurantManagerOrderUpdate(Order order, User manager) {
        if (manager.getRestaurant() == null) {
            throw new ForbiddenException("Current manager does not own any restaurants");
        }
        if (order.getBranch() == null ||
                order.getBranch().getRestaurant() == null ||
                !order.getBranch().getRestaurant().getId().equals(manager.getRestaurant().getId())) {
            throw new ForbiddenException("You are not authorized to update orders outside your restaurant chain.");
        }
    }
}
