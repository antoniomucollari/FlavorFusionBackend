package com.toni.FoodApp.order.service;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.cart.dtos.OrderQuote;
import com.toni.FoodApp.cart.dtos.OrderSummary;
import com.toni.FoodApp.cart.dtos.VariantAvailabilityDTO;
import com.toni.FoodApp.cart.entity.Cart;
import com.toni.FoodApp.cart.entity.CartItem;
import com.toni.FoodApp.cart.repository.CartItemVariantRepository;
import com.toni.FoodApp.cart.repository.CartRepository;
import com.toni.FoodApp.cart.services.CartService;
import com.toni.FoodApp.cart.services.CheckoutService;
import com.toni.FoodApp.deliverTo.dtos.DeliveryInfo;
import com.toni.FoodApp.deliverTo.entity.DeliveryLocation;
import com.toni.FoodApp.deliverTo.repository.DeliveryLocationRepository;
import com.toni.FoodApp.enums.*;
import com.toni.FoodApp.enums.payment.PaymentMethod;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.ForbiddenException;
import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.exceptions.OutOfStockException;
import com.toni.FoodApp.menu.dtos.SimpleMenuDto;
import com.toni.FoodApp.order.Validator.OrderStatusValidator;
import com.toni.FoodApp.order.controller.WebSocketController;
import com.toni.FoodApp.order.dtos.*;
import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.order.entity.OrderItem;
import com.toni.FoodApp.order.entity.OrderItemVariant;
import com.toni.FoodApp.order.event_listener.OrderSavedEvent;
import com.toni.FoodApp.order.mapper.OrderMapper;
import com.toni.FoodApp.order.repository.OrderItemRepository;
import com.toni.FoodApp.order.repository.OrderRepository;
import com.toni.FoodApp.order.specifications.OrderSpecifications;
import com.toni.FoodApp.payment.Specification.PaymentSpecifications;
import com.toni.FoodApp.payment.dtos.request.PaymentSearchCriteria;
import com.toni.FoodApp.payment.dtos.response.PaymentDTO;
import com.toni.FoodApp.payment.dtos.response.PokSdkOrder;
import com.toni.FoodApp.payment.entity.Payment;
import com.toni.FoodApp.payment.repository.PaymentRepository;
import com.toni.FoodApp.payment.service.PaymentServiceImpl;
import com.toni.FoodApp.payment.webhook.service.PokPaymentService;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.entity.BranchMenuItem;
import com.toni.FoodApp.restaurant.repository.RestaurantRepository;
import com.toni.FoodApp.restaurant.repository.ReviewRepository;
import com.toni.FoodApp.util.PaginationUtil;
import com.toni.FoodApp.util.RoleSpecificationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService{
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final CartRepository cartRepository;
    private final DeliveryLocationRepository deliveryLocationRepository;
    private final OrderMapper orderMapper;
    private final RoleSpecificationHelper roleSpecificationHelper;
    private final PaymentRepository paymentRepository;
    private final OrderStatusProcessor orderStatusProcessor;
    private final HelperMethods helperMethods;

    @Transactional
    @Override
    public Response<?> placeOrder(Long branchId) {
        log.info("Inside placeOrderFromCart()");
        User currentUser = userService.getCurrentLoggedInUser();
        Long deliveryLocationId = currentUser.getDeliveryLocation().getId();
        DeliveryLocation deliveryLocation = deliveryLocationRepository.findById(deliveryLocationId)
                .orElseThrow(() -> new BadRequestException("Delivery location not found"));

        Cart cart = cartRepository.findByUserIdAndRestaurantBranchId(currentUser.getId(), branchId)
                .orElseThrow(() -> new BadRequestException("Cart not found"));

        Response<?> existingOrderResponse = helperMethods.handleExistingPendingOrder(currentUser, branchId, cart, deliveryLocation);
        if (existingOrderResponse != null) {
            return existingOrderResponse;
        }

        List<OrderItem> orderItems = helperMethods.validateInventoryAndBuildItems(cart);

        return helperMethods.finalizeNewOrder(currentUser, branchId, cart, deliveryLocation, orderItems);
    }

    @Override
    public Response<Page<OrderDTO>> getALlOrders(OrderSearchCriteria orderSearchCriteria) {
        Map<String, String> orderSortMapping = Map.of(
                "lastUpdated", "lastUpdated",
                "amount", "totalAmount",
                "order_created_date", "orderDate",
                "deliveryDate", "actualDeliveryDate"
        );

        Sort sort = PaginationUtil.buildSort(orderSearchCriteria.getSortBy(), orderSearchCriteria.getSortDirection(), orderSortMapping, "id");
        Pageable pageable = PageRequest.of(orderSearchCriteria.getPage(), orderSearchCriteria.getSize(), sort);

        User currentUser = userService.getCurrentLoggedInUser();

        // Safely determine all applicable roles
        boolean isCustomer = roleSpecificationHelper.isUserThisRole(currentUser, RoleName.CUSTOMER);
        Specification<Order> spec = Specification.allOf(
                OrderSpecifications.hasOrderId(orderSearchCriteria.getOrderId()),
                OrderSpecifications.hasOrderStatus(orderSearchCriteria.getOrderStatus()),
                OrderSpecifications.hasPaymentStatus(orderSearchCriteria.getPaymentStatus()),
                OrderSpecifications.hasCustomerId(orderSearchCriteria.getCustomerId())
        );
        if (isCustomer) {
            spec = spec.and(OrderSpecifications.hasCustomerId(currentUser.getId()));
        } else {
            spec = roleSpecificationHelper.applyRoleBasedSecurity(
                    currentUser,
                    spec,
                    OrderSpecifications::hasRestaurantId,
                    OrderSpecifications::hasBranchId,
                    OrderSpecifications::hasDeliveryId
            );
        }

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        Page<OrderDTO> orderDTOPage = orderPage.map(order -> orderMapper.mapToOrderDTO(order,false, isCustomer,  currentUser.getId()));
        return Response.<Page<OrderDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Orders retrieved successfully.")
                .data(orderDTOPage)
                .build();
    }

    @Override
    public Response<OrderDetailsDto> getOrderDetails(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(()-> new NotFoundException("Order with ID: " + orderId + " Not found"));
        List<Payment> paymentList = Collections.emptyList();
        if(order.getPaymentMethod().equals(PaymentMethod.POK)){
            Specification<Payment> spec = Specification.allOf(
                    PaymentSpecifications.hasOrderId(orderId),
                    PaymentSpecifications.hasCustomerId(order.getUser().getId()));
            paymentList = paymentRepository.findAll(spec);
        }
        OrderDetailsDto orderDetailsDto = orderMapper.mapToOrderDetailsDto(order, paymentList);

        return Response.<OrderDetailsDto>builder()
                .statusCode(HttpStatus.OK.value())
                .message("OrderDetailsDto retrieved successfully.")
                .data(orderDetailsDto)
                .build();
    }


    @Override
    public Response<Page<OrderDTO>> getConfirmedUnassignedOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        List<OrderStatus> incompleteStatuses = getUnassignedStatuses();

        Page<OrderDTO> orderDTOPage = orderRepository
                .findByDeliveryPersonIsNullAndOrderStatusIn(incompleteStatuses, pageable)
                .map(order -> orderMapper.mapToOrderDTO(order,true,false, 1L));

        return Response.<Page<OrderDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Incomplete orders retrieved successfully.")
                .data(orderDTOPage)
                .build();
    }

    @Override
    public Response<OrderItemDTO> getOrderItemById(Long orderItemId) {
        log.info("Inside getOrderItemById() with orderItemId: {}", orderItemId);
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(()-> new NotFoundException("OrderItem with ID: " + orderItemId + " Not found"));

        OrderItemDTO orderItemDTO = modelMapper.map(orderItem, OrderItemDTO.class);

        orderItemDTO.setMenu(modelMapper.map(orderItem.getMenu(), SimpleMenuDto.class));


        return Response.<OrderItemDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("OrderItem retrieved successfully.")
                .data(orderItemDTO)
                .build();
    }
    private List<OrderStatus> getUnassignedStatuses() {
        return List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP);
    }

    private List<PaymentStatus> getIncompletePaymentStatuses() {
        return List.of(PaymentStatus.PENDING_PAYMENT, PaymentStatus.PENDING);
    }
    @Transactional
    @Override
    public Response<OrderDTO> updateStatus(OrderDTO orderDTO) {
        log.info("Updating status for Order ID: {}", orderDTO.getId());

        // Basic Validation
        if (orderDTO.getOrderStatus() != null && orderDTO.getPaymentStatus() != null) {
            throw new BadRequestException("You can only update one status per time.");
        }
        if (orderDTO.getOrderStatus() == null && orderDTO.getPaymentStatus() == null) {
            throw new BadRequestException("Order status or payment status is required.");
        }
        //----------------------------------------

        // Fetch Data
        User user = userService.getCurrentLoggedInUser();
        Order order = orderRepository.findById(orderDTO.getId())
                .orElseThrow(() -> new NotFoundException("Order with ID: " + orderDTO.getId() + " Not found"));

        boolean wasUnassigned = order.getDeliveryPerson() == null && getUnassignedStatuses().contains(order.getOrderStatus());
        if(orderDTO.getOrderStatus() == OrderStatus.DELIVERED){
            order.setPickedUpAt(LocalDateTime.now());
        }
        // call the processor helper methods
        orderStatusProcessor.processStatusUpdate(order, orderDTO, user);

        orderRepository.save(order); // Cascades payment updates automatically

        OrderDTO dto = orderMapper.mapToOrderDTO(order,false, false, null);
        if (order.getDriverEarnings() != null) {
            dto.setDeliveryEarnings(order.getDriverEarnings());
        }

        eventPublisher.publishEvent(new OrderSavedEvent(this, dto, wasUnassigned, order.getBranch().getId()));

        return Response.<OrderDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order updated successfully.")
                .data(dto)
                .build();
    }


    @Override
    public Response<?> assignOrderToDeliveryPerson(Long orderId) {
        User currentUser = userService.getCurrentLoggedInUser();
        if(! roleSpecificationHelper.isUserThisRole(currentUser, RoleName.DELIVERY)) throw new ForbiddenException("You are not authorized to assign orders.");
        Order order = orderRepository.findById(orderId).orElseThrow(()-> new NotFoundException("Order with ID: " + orderId + " Not found"));
        //make sure the order is not already assigned to a delivery person
        if(order.getDeliveryPerson() != null) throw new BadRequestException("Order is already assigned to a delivery person.");
        order.setDeliveryPerson(currentUser);
        orderRepository.save(order);
        return Response.<OrderItemDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order was assigned successfully.")
                .data(modelMapper.map(order, OrderItemDTO.class))
                .build();
    }


    @Override
    public Response<Map<String, Object>> countUniqueCustomers(Long restaurantId) {
        log.info("Counting unique customers. Filter by Restaurant ID: {}", restaurantId);

        LocalDate now = LocalDate.now();
        LocalDateTime nowTime = LocalDateTime.now();

        LocalDateTime currentStart = now.withDayOfMonth(1).atStartOfDay();

        LocalDateTime prevStart = currentStart.minusMonths(1);
        LocalDateTime prevEnd = nowTime.minusMonths(1);
        OrderRepository.CustomerStatsView stats = orderRepository.getCustomerStats(
                currentStart, nowTime, prevStart, prevEnd, restaurantId
        );

        Map<String, Object> data = getStringObjectMap(stats);

        return Response.<Map<String, Object>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Unique customer statistics retrieved successfully.")
                .data(data)
                .build();
    }

    @NotNull
    private static Map<String, Object> getStringObjectMap(OrderRepository.CustomerStatsView stats) {
        long totalUnique = stats.getTotalUnique() != null ? stats.getTotalUnique() : 0;
        long currentMtd = stats.getCurrentMtd() != null ? stats.getCurrentMtd() : 0;
        long prevMtd = stats.getPreviousMtd() != null ? stats.getPreviousMtd() : 0;

        // 4. Calculate Percentage
        double percentageDifference = 0.0;
        if (prevMtd == 0) {
            if (currentMtd > 0) percentageDifference = 100.0;
        } else {
            percentageDifference = ((double) (currentMtd - prevMtd) / prevMtd) * 100;
        }

        // 5. Prepare Response
        Map<String, Object> data = new HashMap<>();
        data.put("totalUniqueCustomers", totalUnique);
        data.put("currentMonthCustomers", currentMtd);
        data.put("previousMonthCustomers", prevMtd);
        data.put("percentageDifference", percentageDifference);
        return data;
    }


    @Override
    public Response<?> countTotalOrders() {
        log.info("Counting total orders with month comparison.");
        User user = userService.getCurrentLoggedInUser();

        boolean isDelivery =  roleSpecificationHelper.isUserThisRole(user, RoleName.DELIVERY);

        LocalDate now = LocalDate.now();
        LocalDate firstDayOfCurrentMonth = now.withDayOfMonth(1);
        LocalDate firstDayOfPreviousMonth = firstDayOfCurrentMonth.minusMonths(1);
        LocalDate lastDayOfPreviousMonth = firstDayOfCurrentMonth.minusDays(1);

        long currentMonthOrders;
        long previousMonthOrders;
        long totalOrders;

        if (isDelivery) {
            currentMonthOrders = orderRepository.count((root, query, cb) ->
                    cb.and(
                            cb.equal(root.get("deliveryPerson").get("id"), user.getId()),
                            cb.equal(root.get("orderStatus"), OrderStatus.DELIVERED),
                            cb.between(root.get("orderDate"), firstDayOfCurrentMonth.atStartOfDay(), now.plusDays(1).atStartOfDay())
                    )
            );

            previousMonthOrders = orderRepository.count((root, query, cb) ->
                    cb.and(
                            cb.equal(root.get("deliveryPerson").get("id"), user.getId()),
                            cb.equal(root.get("orderStatus"), OrderStatus.DELIVERED),
                            cb.between(root.get("orderDate"), firstDayOfPreviousMonth.atStartOfDay(), lastDayOfPreviousMonth.plusDays(1).atStartOfDay())
                    )
            );

            totalOrders = orderRepository.count((root, query, cb) ->
                    cb.and(
                            cb.equal(root.get("deliveryPerson").get("id"), user.getId()),
                            cb.equal(root.get("orderStatus"), OrderStatus.DELIVERED)
                    )
            );
        } else {
            currentMonthOrders = orderRepository.count((root, query, cb) ->
                    cb.between(root.get("orderDate"), firstDayOfCurrentMonth.atStartOfDay(), now.plusDays(1).atStartOfDay())
            );

            previousMonthOrders = orderRepository.count((root, query, cb) ->
                    cb.between(root.get("orderDate"), firstDayOfPreviousMonth.atStartOfDay(), lastDayOfPreviousMonth.plusDays(1).atStartOfDay())
            );

            totalOrders = orderRepository.count(); // counts all orders
        }

        double percentageDifference = 0.0;
        if (previousMonthOrders == 0 && currentMonthOrders > 0) {
            percentageDifference = 100.0;
        } else if (previousMonthOrders > 0) {
            percentageDifference = ((double) (currentMonthOrders - previousMonthOrders) / previousMonthOrders) * 100;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("currentMonthOrders", currentMonthOrders);
        data.put("previousMonthOrders", previousMonthOrders);
        data.put("percentageDifference", percentageDifference);
        data.put("totalOrders", totalOrders);

        return Response.<Map<String, Object>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order statistics retrieved successfully.")
                .data(data)
                .build();
    }


    @Override
    public Response<Map<String, Object>> calculateTotalRevenue() {
        log.info("Calculating total, current, and previous month revenue.");

        // Define date ranges
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfCurrentMonth = now.withDayOfMonth(1);
        LocalDate firstDayOfPreviousMonth = firstDayOfCurrentMonth.minusMonths(1);
        LocalDate lastDayOfPreviousMonth = firstDayOfCurrentMonth.minusDays(1);

        // Total revenue (all-time)
        BigDecimal totalRevenue = orderRepository.sumTotalAmountByStatus(OrderStatus.DELIVERED);

        // Current month revenue
        BigDecimal currentMonthRevenue = orderRepository.sumTotalAmountByStatusAndDateRange(
                OrderStatus.DELIVERED,
                firstDayOfCurrentMonth.atStartOfDay(),
                now.plusDays(1).atStartOfDay()
        );

        // Previous month revenue
        BigDecimal previousMonthRevenue = orderRepository.sumTotalAmountByStatusAndDateRange(
                OrderStatus.DELIVERED,
                firstDayOfPreviousMonth.atStartOfDay(),
                lastDayOfPreviousMonth.plusDays(1).atStartOfDay()
        );

        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        if (currentMonthRevenue == null) currentMonthRevenue = BigDecimal.ZERO;
        if (previousMonthRevenue == null) previousMonthRevenue = BigDecimal.ZERO;

        // Calculate percentage difference
        BigDecimal percentageDifference = BigDecimal.ZERO;
        if (previousMonthRevenue.compareTo(BigDecimal.ZERO) == 0 && currentMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
            percentageDifference = BigDecimal.valueOf(100);
        } else if (previousMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
            percentageDifference = currentMonthRevenue
                    .subtract(previousMonthRevenue)
                    .divide(previousMonthRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Build response data
        Map<String, Object> data = new HashMap<>();
        data.put("totalRevenue", totalRevenue);
        data.put("currentMonthRevenue", currentMonthRevenue);
        data.put("previousMonthRevenue", previousMonthRevenue);
        data.put("percentageDifference", percentageDifference);

        return Response.<Map<String, Object>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Revenue statistics retrieved successfully.") //Profile data retrieved
                .data(data)
                .build();
    }



    @Override
    public Response<Map<String, Long>> getOrderStatusDistribution() {
        log.info("Fetching order status distribution.");
        try {
            List<Object[]> results = orderRepository.countOrdersByStatus();
            Map<String, Long> distribution = results.stream()
                    .collect(Collectors.toMap(
                            // Key: The OrderStatus enum name
                            result -> ((OrderStatus) result[0]).name(),
                            // Value: The count of orders
                            result -> (Long) result[1]
                    ));

            // Ensure all statuses are present in the map, even if their count is 0
            for (OrderStatus status : OrderStatus.values()) {
                distribution.putIfAbsent(status.name(), 0L);
            }

            return Response.<Map<String, Long>>builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Order status distribution fetched successfully.")
                    .data(distribution)
                    .build();
        } catch (Exception e) {
            log.error("Error fetching order status distribution: {}", e.getMessage());
            return Response.<Map<String, Long>>builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Failed to fetch order status distribution.")
                    .build();
        }
    }







}

