package com.example.agentservice_choreography.events.agent.failed;

import java.util.List;
import com.example.agentservice_choreography.model.order.OrderItemDto;

public class AgentAssignmentFailedEventData {
    private String orderId;
    private Long customerId;
    private List<OrderItemDto> orderItems;

    public AgentAssignmentFailedEventData(String orderId, Long customerId, List<OrderItemDto> orderItems) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderItems = orderItems;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public AgentAssignmentFailedEventData() {
    }

    public List<OrderItemDto> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemDto> orderItems) {
        this.orderItems = orderItems;
    }
}