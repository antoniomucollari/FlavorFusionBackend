package com.toni.FoodApp.order.controller;
import com.toni.FoodApp.order.dtos.*;
import com.toni.FoodApp.order.service.OrderService;
import com.toni.FoodApp.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor //only works when there are final fields
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout/{branchId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<?>> checkout(@PathVariable Long branchId){
        return ResponseEntity.ok(orderService.placeOrder(branchId));
    }

    @GetMapping("/order-item/{orderItemId}")
    public ResponseEntity<Response<OrderItemDTO>> getOrderItemById(@PathVariable Long orderItemId){
        return ResponseEntity.ok(orderService.getOrderItemById(orderItemId));
    }

    @GetMapping("/all")
    public ResponseEntity<Response<Page<OrderDTO>>> getAll(
            OrderSearchCriteria orderSearchCriteria
    ){
        return ResponseEntity.ok(orderService.getALlOrders(orderSearchCriteria));
    }


    @PutMapping("/update-status")
    public ResponseEntity<Response<OrderDTO>> updateOrderStatus(@RequestBody OrderDTO orderDTO){
        return ResponseEntity.ok(orderService.updateStatus(orderDTO));
    }


    @PutMapping("/assign-order-delivery/{orderId}")
    @PreAuthorize("hasAuthority('DELIVERY')")
    public ResponseEntity<?> assignOrderToDeliveryPerson(@PathVariable Long orderId){
        return ResponseEntity.ok(orderService.assignOrderToDeliveryPerson(orderId));
    }
    //stats
    @GetMapping("/unique-customers")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ResponseEntity<Response<Map<String, Object>>> countUniqueCustomers(@RequestParam(required = false) Long restaurantId){
        return ResponseEntity.ok(orderService.countUniqueCustomers(restaurantId));
    }
    @GetMapping("/stats/total-orders")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('DELIVERY')")
    public ResponseEntity<Response<?>> getTotalOrders() {
        return ResponseEntity.ok(orderService.countTotalOrders());
    }

    @GetMapping("/stats/total-revenue")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<Map<String, Object>>> getTotalRevenue() {
        return ResponseEntity.ok(orderService.calculateTotalRevenue());
    }


    /**
     * For the "Order Status Distribution" pie chart.
     */
    @GetMapping("/stats/status-distribution")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<Map<String, Long>>> getOrderStatusDistribution() {
        return ResponseEntity.ok(orderService.getOrderStatusDistribution());
    }


    //for the live orders
    @GetMapping("/unassigned-orders")
    @PreAuthorize("hasAuthority('DELIVERY')")
    public ResponseEntity<Response<Page<OrderDTO>>> getConfirmedUnassignedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        return ResponseEntity.ok(orderService.getConfirmedUnassignedOrders(page, size));
    }

    @GetMapping("/order-details/{orderId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<OrderDetailsDto>> getOrderDetails(@PathVariable Long orderId){
        return ResponseEntity.ok(orderService.getOrderDetails(orderId));
    }


}
