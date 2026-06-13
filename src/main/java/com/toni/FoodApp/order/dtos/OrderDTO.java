package com.toni.FoodApp.order.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.toni.FoodApp.auth_users.dtos.UserDTO;
import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.enums.WebSocketAction;
import com.toni.FoodApp.restaurant.dtos.response.BranchCoordinates;
import jakarta.annotation.Nullable;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDTO {
    private Long id;

    private LocalDateTime orderDate;
    private BigDecimal totalAmount;

    private OrderStatus orderStatus;

    private PaymentStatus paymentStatus;

    private UserDTO user;
    private List<OrderItemDTO> orderItems;

    private UserDTO deliveryPerson;
    private Double latitude;
    private Double longitude;
    private Double distanceInKm;
    private String address;

    private String paymentUrl;
    private String paymentGatewayOrderId;

    @Nullable
    private Boolean reviewed;

    private String branchFullName;
    private String imageUrl;
    private String paymentMethod;

    private WebSocketAction wsAction;

    private BigDecimal deliveryEarnings;

    private String deliveryNote;

    private Float est_avg_delivery_time_in_minutes;

    private Long distanceInMeters;
    private BranchCoordinates branchCoordinates;
}
