package com.example.agentservice_choreography.events.agent.failed;

import com.example.agentservice_choreography.events.Event;
import com.example.agentservice_choreography.events.EventType;

public class AgentAssignmentFailedEvent extends Event<AgentAssignmentFailedEventData> {
    public AgentAssignmentFailedEvent(AgentAssignmentFailedEventData eventData) {
        super(EventType.AGENT_ASSIGNMENT_FAILED, eventData);
    }
}

