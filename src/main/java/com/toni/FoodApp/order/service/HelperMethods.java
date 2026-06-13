package com.toni.FoodApp.order.service;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.cart.dtos.OrderQuote;
import com.toni.FoodApp.cart.dtos.OrderSummary;
import com.toni.FoodApp.cart.dtos.VariantAvailabilityDTO;
import com.toni.FoodApp.cart.entity.Cart;
import com.toni.FoodApp.cart.entity.CartItem;
import com.toni.FoodApp.cart.repository.CartItemVariantRepository;
import com.toni.FoodApp.cart.services.CartService;
import com.toni.FoodApp.cart.services.CheckoutService;
import com.toni.FoodApp.deliverTo.dtos.DeliveryInfo;
import com.toni.FoodApp.deliverTo.entity.DeliveryLocation;
import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.enums.payment.PaymentMethod;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.OutOfStockException;
import com.toni.FoodApp.order.dtos.OrderDTO;
import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.order.entity.OrderItem;
import com.toni.FoodApp.order.entity.OrderItemVariant;
import com.toni.FoodApp.order.event_listener.OrderSavedEvent;
import com.toni.FoodApp.order.mapper.OrderMapper;
import com.toni.FoodApp.order.repository.OrderItemRepository;
import com.toni.FoodApp.order.repository.OrderRepository;
import com.toni.FoodApp.payment.dtos.response.PokSdkOrder;
import com.toni.FoodApp.payment.entity.Payment;
import com.toni.FoodApp.payment.repository.PaymentRepository;
import com.toni.FoodApp.payment.webhook.service.PokPaymentService;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.entity.BranchMenuItem;
import com.toni.FoodApp.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Component
public class HelperMethods {
    private final PokPaymentService pokPaymentService;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RestaurantRepository restaurantRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderMapper orderMapper;
    private final CartService cartService;
    private final CartItemVariantRepository cartItemVariantRepository;
    private final CheckoutService checkoutService;
    private final ModelMapper modelMapper;

    public Response<?> handleExistingPendingOrder(User currentUser, Long branchId, Cart cart, DeliveryLocation deliveryLocation) {
        Optional<Order> existingOrderOpt = orderRepository.findFirstByUserIdAndBranchIdAndPaymentStatusOrderByIdDesc(
                currentUser.getId(), branchId, PaymentStatus.PENDING_PAYMENT);
        if (existingOrderOpt.isPresent()) {
            Order existingOrder = existingOrderOpt.get();

            // Check if the existing order is in a terminal state
            boolean isTerminalState = (existingOrder.getOrderStatus() == OrderStatus.FAILED ||
                    existingOrder.getPaymentStatus() == PaymentStatus.ABANDONED);

            if (!isTerminalState) {
                String currentHash = generateCartHash(cart, deliveryLocation);

                if (existingOrder.getCartHash() != null && existingOrder.getCartHash().equals(currentHash)) {
                    Payment latestPayment = existingOrder.getPayment().stream()
                            .max(Comparator.comparing(Payment::getId))
                            .orElse(null);

                    if (latestPayment != null && latestPayment.getExpiresAt().isAfter(Instant.now().plus(5, ChronoUnit.MINUTES))) { //is not null and not expired
                        //added 5 min to fix Dead on Arrival
                        PokSdkOrder pokOrderResponse = new PokSdkOrder();
                        pokOrderResponse.getSelf().setConfirmUrl(latestPayment.getPaymentUrl());
                        return Response.builder()
                                .statusCode(HttpStatus.OK.value())
                                .message("Order already exists, complete your payment.")
                                .data(pokOrderResponse)
                                .build();

                    }
                    if (latestPayment != null) {
                        latestPayment.setPaymentStatus(PaymentStatus.EXPIRED);
                        paymentRepository.save(latestPayment);
                        PokSdkOrder pokOrderResponse = pokPaymentService.initiatePayment(existingOrder);
                        return Response.builder()
                                .statusCode(HttpStatus.OK.value())
                                .message("Previous payment expired. New payment link generated.")
                                .data(pokOrderResponse)
                                .build();
                    }

                } else {
                    // Cart changed — abandon old order and fall through to create new
                    existingOrder.setOrderStatus(OrderStatus.FAILED);
                    existingOrder.setPaymentStatus(PaymentStatus.ABANDONED);
                    if (existingOrder.getPayment() != null) {
                        existingOrder.getPayment().forEach(payment -> payment.setPaymentStatus(PaymentStatus.ABANDONED));
                    }
                    orderRepository.save(existingOrder);
                }
            } else {
                log.info("Existing order {} is in a terminal state (FAILED/ABANDONED). Proceeding to create a new order.", existingOrder.getId());
            }
        }
        return null;
    }

    public List<OrderItem> validateInventoryAndBuildItems(Cart cart) {
        List<CartItem> cartItems = cart.getCartItems();
        if (cartItems.isEmpty()) throw new BadRequestException("Cart is empty");

        List<Long> cartItemIds = cartItems.stream().map(CartItem::getId).toList();
        List<VariantAvailabilityDTO> availabilityList = cartItemVariantRepository.checkAvailabilityForCartItems(cartItemIds);
        Map<Long, List<VariantAvailabilityDTO>> availabilityMap = availabilityList.stream()
                .collect(Collectors.groupingBy(VariantAvailabilityDTO::getCartItemId));

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            List<VariantAvailabilityDTO> variants = availabilityMap.get(cartItem.getId());

            if (variants != null && !variants.isEmpty()) {
                boolean anyUnavailable = variants.stream().anyMatch(v -> !v.getIsAvailable());
                if (anyUnavailable) {
                    throw new OutOfStockException("One or more variants for item " + cartItem.getId() + " are unavailable.");
                }
            }

            BranchMenuItem branchMenuItem = cartItem.getBranchMenuItem();
            OrderItem orderItem = OrderItem.builder()
                    .menu(branchMenuItem.getMenu())
                    .quantity(cartItem.getQuantity())
                    .itemName(branchMenuItem.getMenu().getName())
                    .pricePerUnit(branchMenuItem.getPrice())
                    .subTotal(cartItem.getSubTotal())
                    .build();

            if (variants != null && !variants.isEmpty()) {
                orderItem.setVariants(new ArrayList<>());
                for (VariantAvailabilityDTO variantDto : variants) {
                    OrderItemVariant variantEntity = orderMapper.mapToOrderItemVariant(variantDto, orderItem);
                    variantEntity.setOrderItem(orderItem);
                    orderItem.getVariants().add(variantEntity);
                }
            }
            orderItems.add(orderItem);
        }
        return orderItems;
    }

    public Response<?> finalizeNewOrder(User currentUser, Long branchId, Cart cart, DeliveryLocation deliveryLocation, List<OrderItem> orderItems) {
        OrderQuote orderQuote = checkoutService.buildOrderQuote(cart);
        OrderSummary orderSummary = orderQuote.getOrderSummary();
        DeliveryInfo deliveryInfo = orderQuote.getDeliveryInfo();

        PaymentMethod paymentMethod = cart.getSelectedPaymentMethod().getPaymentMethod();
        PaymentStatus initialPaymentStatus = (paymentMethod == PaymentMethod.POK) ? PaymentStatus.PENDING_PAYMENT : PaymentStatus.PENDING;

        Order order = orderMapper.mapToOrder(orderSummary, currentUser, orderItems, deliveryLocation, deliveryInfo, cart, calculateAverageTime(deliveryInfo.getDeliveryTime()), initialPaymentStatus, paymentMethod);
        order.setOrderDate(LocalDateTime.now());
        order.setCartHash(generateCartHash(cart, deliveryLocation));
        order.setDriverEarnings(calculateDriverEarnings(order.getDeliveryPrice(), order.getTipAmount()));

        Order savedOrder = orderRepository.save(order);
        orderItems.forEach(orderItem -> orderItem.setOrder(savedOrder));
        orderItemRepository.saveAll(orderItems);

        if (paymentMethod == PaymentMethod.POK) {
            PokSdkOrder pokOrderResponse = pokPaymentService.initiatePayment(savedOrder);
            return Response.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Payment initiated. Please complete payment.")
                    .data(pokOrderResponse)
                    .build();
        } else {
            cartService.clearShoppingCart(branchId, null);
            OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);
            eventPublisher.publishEvent(new OrderSavedEvent(this, orderDTO, false, order.getBranch().getId()));
            restaurantRepository.incrementDailyOrderCount(branchId);

            return Response.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Order placed successfully")
                    .data(orderDTO)
                    .build();
        }
    }

    public void setCartHash(Cart cart, DeliveryLocation deliveryLocation, Order order) {
        order.setCartHash(generateCartHash(cart, deliveryLocation));
    }
    public PokSdkOrder initiatePayment(Order order) {
        return pokPaymentService.initiatePayment(order);
    }

    private String generateCartHash(Cart cart, DeliveryLocation deliveryLocation) {
        // Sort to ensure order doesn't matter
        String itemsHash = cart.getCartItems().stream()
                .sorted(Comparator.comparing(ci -> ci.getBranchMenuItem().getId()))
                .map(ci -> ci.getBranchMenuItem().getId() + ":" + ci.getQuantity() + ":" + ci.getSubTotal())
                .collect(Collectors.joining("|"));

        String full = itemsHash
                + "_tip:" + cart.getTipAmount()
                + "_loc:" + deliveryLocation.getId()
                + "_pay:" + cart.getSelectedPaymentMethod().getPaymentMethod();

        return DigestUtils.md5DigestAsHex(full.getBytes());
    }
    private Float calculateAverageTime(String deliveryTimeStr) {
        if (deliveryTimeStr == null || !deliveryTimeStr.contains("-")) {
            return null;
        }
        try {
            String cleanStr = deliveryTimeStr.replace(" min", "").trim();
            String[] parts = cleanStr.split("-");
            return (Float.parseFloat(parts[0].trim()) + Float.parseFloat(parts[1].trim())) / 2.0f;
        } catch (Exception e) {
            return null;
        }
    }

    public OrderQuote setCheckoutService(Cart cart){
        return checkoutService.buildOrderQuote(cart);
    }
    private BigDecimal calculateDriverEarnings(BigDecimal deliveryPrice, BigDecimal tip) {
        BigDecimal threshold = new BigDecimal("80");
        BigDecimal deliveryCut;

        if (deliveryPrice.compareTo(threshold) <= 0) {
            deliveryCut = deliveryPrice; // 100%
        } else {
            deliveryCut = deliveryPrice.multiply(new BigDecimal("0.5"));
        }

        return deliveryCut.add(tip);
    }
}
