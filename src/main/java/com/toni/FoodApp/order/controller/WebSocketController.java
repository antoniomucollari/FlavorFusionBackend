package com.toni.FoodApp.order.controller;

import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.enums.WebSocketAction;
import com.toni.FoodApp.order.dtos.OrderDTO;
import com.toni.FoodApp.order.event_listener.OrderSavedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleOrderSaveEvent(OrderSavedEvent event) {
        OrderDTO order = event.getOrderDTO();
        Long branchId = event.getBranchId();

        // 1. Branch Manager Broadcast
        if (branchId != null) {
            // ✅ FIXED: Changed slashes to dots for RabbitMQ Topic Exchange
            messagingTemplate.convertAndSend("/topic/branch." + branchId + ".manager", order);
            handleGlobalUnassignedUpdate(event, order);
        }

        // 2. Personal Driver Update
        if (order.getDeliveryPerson() != null) {
            String driverEmail = order.getDeliveryPerson().getEmail();
            messagingTemplate.convertAndSendToUser(driverEmail, "/queue/updates", order);
        }
    }

    private void handleGlobalUnassignedUpdate(OrderSavedEvent event, OrderDTO order) {
        String globalTopic = "/topic/delivery.global.unassigned";

        boolean isNowUnassigned = isUnassignedCriteriaMet(order);
        boolean wasPreviouslyUnassigned = event.isWasPreviouslyUnassigned();

        if (isNowUnassigned) {
            order.setWsAction(WebSocketAction.ADD);
            messagingTemplate.convertAndSend(globalTopic, order);
        }
        else if (wasPreviouslyUnassigned) {
            order.setWsAction(WebSocketAction.REMOVE);
            messagingTemplate.convertAndSend(globalTopic, order);
        }
    }

    private boolean isUnassignedCriteriaMet(OrderDTO order) {
        if (order.getDeliveryPerson() != null) return false;

        return order.getOrderStatus() == OrderStatus.CONFIRMED
                || order.getOrderStatus() == OrderStatus.PREPARING
                || order.getOrderStatus() == OrderStatus.READY_FOR_PICKUP;
    }
}