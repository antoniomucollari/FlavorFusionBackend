package com.toni.FoodApp.payment.Specification;

import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.payment.entity.Payment;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class PaymentSpecifications {

    public static Specification<Payment> hasCustomerId(Long customerId) {
        return (root, query, cb) ->
                customerId == null ? null : cb.equal(root.get("order").get("user").get("id"), customerId);
    }

    public static Specification<Payment> hasOrderId(Long orderId){
        return (root, query, cb) ->
                orderId == null ? null : cb.equal(root.get("order").get("id"), orderId);
    }

    public static Specification<Payment> hasTransactionId(String transactionId){
        return (root, query, cb) ->
                transactionId == null ? null : cb.equal(root.get("payment").get("transactionId"), transactionId);
    }

    public static Specification<Payment> hasPaymentStatus(List<PaymentStatus> paymentStatuses) {
        return (root, query, cb) ->
                (paymentStatuses == null || paymentStatuses.isEmpty())
                        ? null
                        // Updated to match your exact field name
                        : root.get("paymentStatus").in(paymentStatuses);
    }

    // Updated parameter to Long to match your entity's @Id
    public static Specification<Payment> hasPaymentId(Long paymentId) {
        return (root, query, cb) ->
                paymentId == null ? null : cb.equal(root.get("id"), paymentId);
    }

    // --- Role-Based Traversals through the Order entity ---

    public static Specification<Payment> hasDeliveryId(Long deliveryId) {
        return (root, query, cb) ->
                deliveryId == null ? null : cb.equal(root.get("order").get("deliveryPerson").get("id"), deliveryId);
    }

    public static Specification<Payment> hasBranchId(Long branchId) {
        return (root, query, cb) ->
                branchId == null ? null : cb.equal(root.get("order").get("branch").get("id"), branchId);
    }

    public static Specification<Payment> hasRestaurantId(Long restaurantId) {
        return (root, query, cb) ->
                restaurantId == null ? null : cb.equal(root.get("order").get("branch").get("restaurant").get("id"), restaurantId);
    }
}