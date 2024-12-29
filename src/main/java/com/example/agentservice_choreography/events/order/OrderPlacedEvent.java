package com.example.agentservice_choreography.events.order;

import com.example.agentservice_choreography.events.Event;
import com.example.agentservice_choreography.events.EventType;

public class OrderPlacedEvent extends Event<OrderPlacedEventData> {
    public OrderPlacedEvent(OrderPlacedEventData orderPlacedEventData) {
        super(EventType.ORDER_PLACED, orderPlacedEventData);
    }
}
