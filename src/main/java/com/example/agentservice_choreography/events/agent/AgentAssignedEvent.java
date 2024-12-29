package com.example.agentservice_choreography.events.agent;

import com.example.agentservice_choreography.events.Event;
import com.example.agentservice_choreography.events.EventType;
import com.example.agentservice_choreography.events.inventory.InventoryReservedEventData;

public class AgentAssignedEvent extends Event<AgentAssignedEventData> {
    public AgentAssignedEvent(AgentAssignedEventData agentAssignedEventData) {
        super(EventType.AGENT_ASSIGNED, agentAssignedEventData);
    }
}
