package com.toni.FoodApp.payment.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toni.FoodApp.cart.services.CartService;
import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.enums.payment.PaymentGateway;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.exceptions.PaymentGatewayException;
import com.toni.FoodApp.order.dtos.OrderDTO;
import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.order.event_listener.OrderSavedEvent;
import com.toni.FoodApp.order.mapper.OrderMapper;
import com.toni.FoodApp.order.repository.OrderRepository;
import com.toni.FoodApp.payment.dtos.request.PokLoginRequest;
import com.toni.FoodApp.payment.dtos.request.PokOrderRequest;
import com.toni.FoodApp.payment.dtos.request.PokRefundRequest;
import com.toni.FoodApp.payment.dtos.response.PokApiOrderResponse;
import com.toni.FoodApp.payment.dtos.response.PokLoginResponse;
import com.toni.FoodApp.payment.dtos.response.PokSdkOrder;
import com.toni.FoodApp.payment.entity.Payment;
import com.toni.FoodApp.payment.mapper.PaymentMethodMapper;
import com.toni.FoodApp.payment.repository.PaymentRepository;
import com.toni.FoodApp.payment.dtos.request.PokWebhookPayload;
import com.toni.FoodApp.payment.webhook.httpRequest.PostRequest;
import com.toni.FoodApp.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor

public class PokPaymentServiceImpl implements PokPaymentService{
    private final ApplicationEventPublisher eventPublisher;
    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    @Value("${pok.api.baseUrl}")
    private String basePaymentLink;
    private final RestaurantRepository restaurantRepository;
    @Value("${pok.api.baseUrl}")
    private String baseUrl;
    @Value("${pok.api.keyId}")
    private String keyId;
    @Value("${pok.api.keySecret}")
    private String keySecret;
    @Value("${pok.api.merchantId}")
    private String merchantId;

    private final PaymentMethodMapper paymentMethodMapper;
    private final PaymentRepository paymentRepository; // Create this repository for your Payment entity
    private final PostRequest postRequest;
    private final CartService cartService;
    @Transactional
    public void processSuccessfulPayment(PokWebhookPayload payload)  {
        log.info("inside processSuccessfulPayment with payload: {}", payload);

        Order order = orderRepository.findByPaymentTransactionId(payload.getTransactionId())
                .orElseThrow(() -> new NotFoundException("Payment not found with transactionId: " + payload.getTransactionId()));

        // 1. IDEMPOTENCY GUARD: If we already processed this webhook, stop here!
        if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            log.info("Webhook duplicate: Payment already marked as COMPLETED for transaction {}", payload.getTransactionId());
            return;
        }

        Payment payment = paymentRepository.findByTransactionId(payload.getTransactionId())
                .orElse(buildPayment(order, PaymentStatus.COMPLETED, LocalDateTime.now(), payload.getTransactionId(),null));

        // 2. RESURRECT THE ORDER (If the cron job killed it)
        if (order.getOrderStatus() == OrderStatus.FAILED) {
            log.info("Resurrecting FAILED order {} back to INITIALIZED due to successful late payment.", order.getId());
            order.setOrderStatus(OrderStatus.INITIALIZED);
        }

        order.setPaymentStatus(PaymentStatus.COMPLETED);

        if (!order.getPayment().contains(payment)) {
            order.getPayment().add(payment);
        }

        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        cartService.clearShoppingCart(order.getBranch().getId(), order.getUser().getId());
        restaurantRepository.incrementDailyOrderCount(order.getBranch().getId());

        OrderDTO orderDTO = orderMapper.mapToOrderDTO(order, false,false, null);
        eventPublisher.publishEvent(new OrderSavedEvent(this, orderDTO, false, order.getBranch().getId()));
    }

    private Payment buildPayment(Order order, PaymentStatus status, LocalDateTime dateTime, String transactionId, String url ){
        return Payment.builder()
                .order(order)
                .transactionId(transactionId)
                .paymentStatus(status)
                .amount(order.getTotalAmount())
                .paymentDate(dateTime)
                .paymentUrl(url)
                .paymentGateway(PaymentGateway.POK)
                .build();
    }

    public String login() {
        String url = baseUrl + "auth/sdk/login";
        PokLoginRequest request = new PokLoginRequest(keyId, keySecret);

        try {
            ResponseEntity<PokLoginResponse> response = restTemplate.postForEntity(url, request, PokLoginResponse.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().getData().getAccessToken();
            }
            throw new PaymentGatewayException("Failed to login   to Pok API. Invalid response.");
        } catch (Exception e) {
            throw new PaymentGatewayException("Error during Pok API login: " + e.getMessage());
        }
    }
    @Override
    public PokSdkOrder initiatePayment(Order order) {

        String token = login();
        String url = baseUrl + "/merchants/" + merchantId + "/sdk-orders";
        log.info("Using token to create order: {}", token);

        PokOrderRequest pokOrderRequest = paymentMethodMapper.buildPokOrderRequest(order);

        PokApiOrderResponse orderResponse = postRequest.executePostRequest(
                url,
                pokOrderRequest,
                token,
                PokApiOrderResponse.class,
                "Pok order"
        );
        String transactionId = orderResponse.getData().getSdkOrder().getId();
        String paymentUrl = orderResponse.getData().getSdkOrder().getSelf().getConfirmUrl();
        Payment payment = buildPayment(order, PaymentStatus.PENDING_PAYMENT, null, transactionId, paymentUrl);

        payment.setCreatedDate(orderResponse.getData().getSdkOrder().getCreatedAt());
        payment.setExpiresAt(orderResponse.getData().getSdkOrder().getExpiresAt());
        paymentRepository.save(payment);

        if (orderResponse.getData() != null && orderResponse.getData().getSdkOrder() != null) {
            return orderResponse.getData().getSdkOrder();
        }

        throw new PaymentGatewayException("Pok API response was successful, but SdkOrder data was missing.");
    }
    @Override
    public PokSdkOrder refundPayment(String sdkOrderId, BigDecimal amount, String reason) {
        String token = login();
        String url = baseUrl + "/merchants/" + merchantId + "/sdk-orders/" + sdkOrderId + "/refund";
        log.info("Initiating refund for order {} at URL: {}", sdkOrderId, url);

        PokRefundRequest refundRequest = PokRefundRequest.builder()
                .refundAmount(amount)
                .refundReason(reason)
                .build();

        try {
            PokApiOrderResponse response = postRequest.executePostRequest(
                    url,
                    refundRequest,
                    token,
                    PokApiOrderResponse.class,
                    "Pok refund"
            );

            if (response != null && response.getData() != null && response.getData().getSdkOrder() != null) {
                PokSdkOrder updatedOrder = response.getData().getSdkOrder();

                if (Boolean.TRUE.equals(updatedOrder.getIsRefunded())) {
                    log.info("Refund successful for order {}", sdkOrderId);
                    return updatedOrder;
                } else {
                    log.warn("Refund request succeeded, but order is marked as not refunded.");
                }
            }

            throw new PaymentGatewayException("Failed to process refund: SDK Order data missing in response.");

        } catch (Exception e) {
            extractAndThrowCleanErrorMessage(e);

            throw e;
        }
    }

    /**
     * Helper method to extract the specific "message" from the Pok API JSON error response.
     */
    private void extractAndThrowCleanErrorMessage(Exception e) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = getString(e);

            // Parse the JSON and extract the "message" field
            if (jsonBody != null) {
                JsonNode rootNode = mapper.readTree(jsonBody);
                if (rootNode.has("message")) {
                    String cleanError = rootNode.get("message").asText();
                    // Throws: "PaymentGatewayException: Order not eligible for refund"
                    throw new PaymentGatewayException(cleanError);
                }
            }
        } catch (PaymentGatewayException pgEx) {
            throw pgEx;
        } catch (Exception parsingEx) {
            log.warn("Could not parse error response from Pok API: {}", parsingEx.getMessage());
        }
    }

    @Nullable
    private static String getString(Exception e) {
        String jsonBody = null;

        if (e.getCause() instanceof HttpStatusCodeException) {
            jsonBody = ((HttpStatusCodeException) e.getCause()).getResponseBodyAsString();
        }

        else if (e.getMessage() != null && e.getMessage().contains("{")) {
            jsonBody = e.getMessage().substring(e.getMessage().indexOf("{"));
        }
        return jsonBody;
    }
}