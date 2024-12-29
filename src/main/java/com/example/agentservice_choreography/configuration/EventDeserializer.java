package com.example.agentservice_choreography.configuration;

import com.example.agentservice_choreography.events.Event;
import com.example.agentservice_choreography.events.EventType;
import com.example.agentservice_choreography.events.agent.AgentAssignedEvent;
import com.example.agentservice_choreography.events.agent.AgentAssignedEventData;
import com.example.agentservice_choreography.events.agent.failed.AgentAssignmentFailedEvent;
import com.example.agentservice_choreography.events.agent.failed.AgentAssignmentFailedEventData;
import com.example.agentservice_choreography.events.inventory.InventoryReservedEvent;
import com.example.agentservice_choreography.events.inventory.InventoryReservedEventData;
import com.example.agentservice_choreography.events.order.OrderPlacedEvent;
import com.example.agentservice_choreography.events.order.OrderPlacedEventData;
import com.example.agentservice_choreography.events.order.cancelled.OrderCancelledEvent;
import com.example.agentservice_choreography.events.order.cancelled.OrderCancelledEventData;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class EventDeserializer extends JsonDeserializer<Event<?>> {
    @Override
    public Event<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = jp.getCodec();
        JsonNode node = codec.readTree(jp);
        EventType eventType = EventType.valueOf(node.get("eventType").asText());

        return switch (eventType) {
            case ORDER_PLACED -> new OrderPlacedEvent(codec.treeToValue(node.get("data"), OrderPlacedEventData.class));
            case INVENTORY_RESERVED ->
                    new InventoryReservedEvent(codec.treeToValue(node.get("data"), InventoryReservedEventData.class));
            case AGENT_ASSIGNED ->
                    new AgentAssignedEvent(codec.treeToValue(node.get("data"), AgentAssignedEventData.class));
            case AGENT_ASSIGNMENT_FAILED ->
                    new AgentAssignmentFailedEvent(codec.treeToValue(node.get("data"), AgentAssignmentFailedEventData.class));
            case ORDER_CANCELLED ->
                    new OrderCancelledEvent(codec.treeToValue(node.get("data"), OrderCancelledEventData.class));
        };
    }
}
