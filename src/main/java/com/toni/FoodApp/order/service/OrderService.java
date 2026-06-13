package com.toni.FoodApp.order.service;

import com.toni.FoodApp.order.dtos.*;
import com.toni.FoodApp.response.Response;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface OrderService {

    Response<?> placeOrder(Long branch);

    Response<Page<OrderDTO>> getALlOrders(OrderSearchCriteria orderSearchCriteria);

    Response<OrderItemDTO> getOrderItemById(Long orderItemId);
    Response<OrderDTO> updateStatus(OrderDTO orderDTO);
    Response<Map<String, Object>> countUniqueCustomers(Long restaurantId);
    Response<?> countTotalOrders();

    Response<Map<String, Object>> calculateTotalRevenue() ;


    Response<Map<String, Long>> getOrderStatusDistribution();

    Response<Page<OrderDTO>> getConfirmedUnassignedOrders(int page, int size);

    Response<?> assignOrderToDeliveryPerson(Long orderId);

    Response<OrderDetailsDto> getOrderDetails(Long orderId);
}
