package com.toni.FoodApp.order.specifications;

import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.order.entity.Order;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class OrderSpecifications {

    public static Specification<Order> hasCustomerId(Long customerId) {
        return (root, query, cb) ->
                customerId == null ? null : cb.equal(root.get("user").get("id"), customerId);
    }

    public static Specification<Order> hasOrderStatus(List<OrderStatus> orderStatuses) {
        return (root, query, cb) ->
                (orderStatuses == null || orderStatuses.isEmpty())
                        ? null
                        : root.get("orderStatus").in(orderStatuses);
    }

    public static Specification<Order> hasPaymentStatus(List<PaymentStatus> paymentStatuses) {
        return (root, query, cb) -> {
            if (paymentStatuses == null || paymentStatuses.isEmpty()) {
                // default: exclude PENDING_PAYMENT silently
                return cb.notEqual(root.get("paymentStatus"), PaymentStatus.ABANDONED);
            }
            return root.get("paymentStatus").in(paymentStatuses);
        };
    }

    public static Specification<Order> hasOrderId(Integer orderId) {
        return (root, query, cb) ->
                orderId == null ? null : cb.equal(root.get("id"), orderId);
    }

    // for delivery user
    public static Specification<Order> hasDeliveryId(Long deliveryId) {
        return (root, query, cb) ->
                deliveryId == null ? null : cb.equal(root.get("deliveryPerson").get("id"), deliveryId);
    }

    public static Specification<Order> hasBranchId(Long branchId) {
        return (root, query, cb) ->
                branchId == null ? null : cb.equal(root.get("branch").get("id"), branchId);
    }

    // For Restaurant Manager
    public static Specification<Order> hasRestaurantId(Long restaurantId) {
        return (root, query, cb) ->
                restaurantId == null ? null : cb.equal(root.get("branch").get("restaurant").get("id"), restaurantId);
    }
    public static Specification<Order> hasDeliveryPersonNull() {
        return (root, query, cb) -> cb.isNull(root.get("deliveryPerson"));
    }
}

