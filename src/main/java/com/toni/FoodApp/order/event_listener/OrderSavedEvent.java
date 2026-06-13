package com.toni.FoodApp.order.event_listener;

import com.toni.FoodApp.order.dtos.OrderDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderSavedEvent extends ApplicationEvent {
    private final OrderDTO orderDTO;
    private final Long branchId;
    private final boolean wasPreviouslyUnassigned;

    public OrderSavedEvent(Object source, OrderDTO orderDTO, boolean wasPreviouslyUnassigned, Long branchId) {
        super(source);
        this.branchId = branchId;
        this.orderDTO = orderDTO;
        this.wasPreviouslyUnassigned = wasPreviouslyUnassigned;
    }
}