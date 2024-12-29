package com.example.agentservice_choreography.events.order.cancelled;

import com.example.agentservice_choreography.events.Event;
import com.example.agentservice_choreography.events.EventType;

public class OrderCancelledEvent extends Event<OrderCancelledEventData> {
    public OrderCancelledEvent(OrderCancelledEventData eventData) {
        super(EventType.ORDER_CANCELLED, eventData);
    }

}
