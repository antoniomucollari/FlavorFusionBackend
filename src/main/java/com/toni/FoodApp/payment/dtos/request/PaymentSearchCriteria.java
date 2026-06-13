package com.toni.FoodApp.payment.dtos.request;

import com.toni.FoodApp.enums.payment.PaymentStatus;
import lombok.Data;
import java.util.List;

@Data
public class PaymentSearchCriteria {
    private List<PaymentStatus> paymentStatus;
    private int page = 0;
    private int size = 10;
    private Long customerId;
    private Long orderId;
    private Long paymentId;
    private Long deliveryId;
    private Long restaurantId;
    private Long branchId;
    private String transactionId;
    private String sortBy = "default";
    private String sortDirection = "desc";
}