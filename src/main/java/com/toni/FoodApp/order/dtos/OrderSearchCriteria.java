package com.toni.FoodApp.order.dtos;
import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import lombok.Data;
import java.util.List;

@Data
public class OrderSearchCriteria {
    private List<OrderStatus> orderStatus;
    private List<PaymentStatus> paymentStatus;
    private int page = 0;
    private int size = 10;
    private Long customerId;
    private Integer orderId;
    private Long deliveryId;
    private String sortBy = "default";
    private String sortDirection = "desc";
}