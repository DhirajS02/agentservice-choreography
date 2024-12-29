package com.example.agentservice_choreography.events.inventory;

import com.example.agentservice_choreography.events.Event;
import com.example.agentservice_choreography.events.EventType;

public class InventoryReservedEvent extends Event<InventoryReservedEventData> {
    public InventoryReservedEvent(InventoryReservedEventData inventoryReservedData)
    {
        super(EventType.INVENTORY_RESERVED,inventoryReservedData);
    }
}
