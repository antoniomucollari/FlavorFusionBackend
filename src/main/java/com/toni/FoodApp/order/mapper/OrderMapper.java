package com.toni.FoodApp.order.mapper;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.cart.dtos.OrderSummary;
import com.toni.FoodApp.cart.dtos.VariantAvailabilityDTO;
import com.toni.FoodApp.cart.entity.Cart;
import com.toni.FoodApp.deliverTo.dtos.DeliveryInfo;
import com.toni.FoodApp.deliverTo.entity.DeliveryLocation;
import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.enums.payment.PaymentMethod;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.menu.repository.OptionRepository;
import com.toni.FoodApp.order.dtos.OrderDTO;
import com.toni.FoodApp.order.dtos.OrderDetailsDto;
import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.order.entity.OrderItem;
import com.toni.FoodApp.order.entity.OrderItemVariant;
import com.toni.FoodApp.payment.dtos.response.PaymentDTO;
import com.toni.FoodApp.payment.entity.Payment;
import com.toni.FoodApp.payment.mapper.PaymentMapper;
import com.toni.FoodApp.restaurant.dtos.response.BranchCoordinates;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.entity.Review;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import com.toni.FoodApp.restaurant.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
@Component
@RequiredArgsConstructor
public class OrderMapper {
    private final ModelMapper modelMapper;
    private final OptionRepository optionRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentMapper paymentMapper;
    private final BranchRepository branchRepository;
    public OrderItemVariant mapToOrderItemVariant(VariantAvailabilityDTO variantAvailabilityDTO, OrderItem orderItem ){
        Long variantId = variantAvailabilityDTO.getId();
        return OrderItemVariant.builder()
                .orderItem(orderItem)
                .variantName(variantAvailabilityDTO.getVariantName())
                .priceCharged(variantAvailabilityDTO.getPrice())
                .originalVariantId(variantId)
                .originalOptionId(optionRepository.getOptionByVariantId(variantId).getId())
                .build();
    }
    public Order mapToOrder(
            OrderSummary orderSummary,
            User currentUser,
            List<OrderItem> orderItems,
            DeliveryLocation deliveryLocation,
            DeliveryInfo deliveryInfo,
            Cart cart,
            Float estAvgDeliveryTimeInMinutes,
            PaymentStatus initialPaymentStatus,
            PaymentMethod paymentMethod

    ){
        return Order.builder()
                .user(currentUser)
                .orderItems(orderItems)

                .serviceFee(orderSummary.getDeliveryFee())
                .totalAmount(orderSummary.getTotalAmount())
                .tipAmount(orderSummary.getTipAmount())
                .subtotal(orderSummary.getSubtotal())
                .deliveryPrice(deliveryInfo.getDeliveryFee())

                .orderStatus(OrderStatus.INITIALIZED)

                .paymentStatus(initialPaymentStatus)
                .paymentMethod(paymentMethod)

                .latitude(deliveryLocation.getLatitude())
                .longitude(deliveryLocation.getLongitude())
                .address(deliveryLocation.getLocationName())
                .deliveryNote(cart.getDeliveryNote())
                .estAvgDeliveryTimeInMinutes(estAvgDeliveryTimeInMinutes)
                .distanceInMeters(deliveryInfo.getDistanceInMeters())

                .branch(cart.getRestaurantBranch())
                .build();
    }
    public OrderDTO mapToOrderDTO(Order order,boolean isDelivery, boolean isCustomer, Long currentUserId) {
        OrderDTO dto = modelMapper.map(order, OrderDTO.class);
        if (dto.getOrderItems() == null) {
            throw new RuntimeException("OrderDTO is null");
        }

        if (isCustomer) {
            RestaurantBranch branch = order.getBranch();
            // Check if reviewed
            dto.setReviewed(reviewRepository.orderExistsInReview(order.getId(), currentUserId));
            dto.setUser(null); // Hide user details from the customer response

            // Attach food delivery app specific branch details
            if (branch != null && branch.getRestaurant() != null) {
                dto.setBranchFullName(branch.getRestaurant().getName() + " " + branch.getAddress());
                dto.setImageUrl(branch.getRestaurant().getProfileImageUrl());
            }

        }
        if(isDelivery){
            RestaurantBranch branch = branchRepository.findByOrderId(order.getId()).orElseThrow(()->new BadRequestException("this order does not have any branch related to it."));
            dto.setDeliveryEarnings(order.getDriverEarnings());
            dto.setDistanceInMeters(order.getDistanceInMeters());
            dto.setEst_avg_delivery_time_in_minutes(order.getEstAvgDeliveryTimeInMinutes());
            dto.setBranchCoordinates(BranchCoordinates.builder()
                    .lat(branch.getLocation().getY())
                    .lng(branch.getLocation().getX())
                    .build());
        }

        return dto;
    }


    public OrderDetailsDto mapToOrderDetailsDto(Order order, List<Payment> paymentList){
        OrderDTO orderDTO = mapToOrderDTO(order,false, true, order.getUser().getId());
        List<PaymentDTO> paymentDtoList = paymentList.stream().map(paymentMapper::mapToPaymentDto).toList();
        Review review = reviewRepository.findByOrderId(order.getId()).orElse(null);
        return OrderDetailsDto.builder()
                .orderDTO(orderDTO)
                .estDeliveryDate(LocalDateTime.now()
                        .plusMinutes(order.getEstAvgDeliveryTimeInMinutes().longValue()))
                .deliveryDate(order.getActualDeliveryDate())
                .pickedUpAt(order.getPickedUpAt())
                .reviewStars(review != null ? review.getRating() : null)
                .reviewMessage(review != null ? review.getComment() : null)
                .paymentDTO(paymentDtoList)
                .serviceFee(order.getServiceFee())
                .reasonOfFailure(order.getReasonOfFailure())
                .build();
    }
}
