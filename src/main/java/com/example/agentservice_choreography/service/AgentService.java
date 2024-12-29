package com.example.agentservice_choreography.service;

import com.example.agentservice_choreography.events.Event;
import com.example.agentservice_choreography.events.agent.AgentAssignedEvent;
import com.example.agentservice_choreography.events.agent.AgentAssignedEventData;
import com.example.agentservice_choreography.events.agent.failed.AgentAssignmentFailedEvent;
import com.example.agentservice_choreography.events.agent.failed.AgentAssignmentFailedEventData;
import com.example.agentservice_choreography.events.inventory.InventoryReservedEventData;
import com.example.agentservice_choreography.exceptions.EventParsingException;
import com.example.agentservice_choreography.exceptions.UnAvailableAgentException;
import com.example.agentservice_choreography.model.order.OrderItemDto;
import com.example.agentservice_choreography.repository.AgentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import com.example.agentservice_choreography.events.order.cancelled.OrderCancelledEventData;
import java.util.List;
import java.util.Map;

@Service
public class AgentService {

    private final SqsClient sqsClient;
    private final String inventoryQueueUrl;
    private final String agentQueueUrl;
    private final String orderCancelledQueueUrl;
    private final String agentFailureQueueUrl;
    private final AgentRepository agentRepository;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);


    public AgentService(SqsClient sqsClient,
                        @Value("${queue.inventory.url}") String inventoryQueueUrl,
                        @Value("${queue.agent.url}") String agentQueueUrl,
                        @Value("${queue.agent.order.cancelled.url}") String orderCancelledQueueUrl,
                        @Value("${queue.agent.failed.url}") String agentFailureQueueUrl,
                        AgentRepository agentRepository,
                        @Qualifier("event-deserializer") ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.inventoryQueueUrl = inventoryQueueUrl;
        this.agentQueueUrl = agentQueueUrl;
        this.orderCancelledQueueUrl = orderCancelledQueueUrl;
        this.agentFailureQueueUrl = agentFailureQueueUrl;
        this.agentRepository = agentRepository;
        this.objectMapper = objectMapper;
    }

    // Method to listen for InventoryReserved events
    @Scheduled(fixedRate = 20000,initialDelay = 20000)
    public void onInventoryReserved(){
        logger.info("Listening to InventoryReserved event");
        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(inventoryQueueUrl)
                .waitTimeSeconds(10)//Long polling, connection will wait for 10 seconds
                .maxNumberOfMessages(10)
                .build();

        final var messages = sqsClient.receiveMessage(receiveRequest).messages();
        messages.forEach(message -> {
            try {
                logger.info("Received event: InventoryReserved with message body = " + message.body());
                // Deserialize the event from JSON message
                final var inventoryReservedEvent =objectMapper.readValue(
                        message.body(),
                        new TypeReference<Event<InventoryReservedEventData>>() {}
                );

                // Process the event and publish Agent Event
                processInventoryReservedEvent(inventoryReservedEvent.getData());
                deleteMessage(message.receiptHandle(),inventoryQueueUrl);
                logger.info("Succesfully deleted event: InventoryReserved with message body = " + message.body());
            }  catch (JsonProcessingException e) {
                logger.error("Failed to parse InventoryReserved event with message ID: {}. Error: {}", message.messageId(),e.getMessage(),e);
            }
            catch (Exception e) {
                logger.error("Failed to process message: {}. Exception: {}", message.body(), e.getMessage(), e);
            }
        });
    }

    @Scheduled(fixedRate = 20000,initialDelay = 20000)
    public void onOrderCancelled() throws EventParsingException {
        logger.info("Listening to OrderCancelled event");
        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(orderCancelledQueueUrl)
                .waitTimeSeconds(10)//Long polling, connection will wait for 10 seconds
                .maxNumberOfMessages(10)
                .build();

        final var messages = sqsClient.receiveMessage(receiveRequest).messages();
        messages.forEach(message -> {
            try {
                logger.info("Received event: OrderCancelled with message body = " + message.body());
                final var outerMessage = objectMapper.readValue(
                        message.body(),
                        new TypeReference<Map<String, Object>>() {}
                );

                // Extract the inner Message
                final String innerMessage = (String) outerMessage.get("Message");
                // Deserialize the inner message into your event object
                final var orderCancelledEventDataEvent = objectMapper.readValue(
                        innerMessage,
                        new TypeReference<Event<OrderCancelledEventData>>() {}
                );
                processCompensatingTransaction(orderCancelledEventDataEvent.getData().getDeliveryAgentId());
                deleteMessage(message.receiptHandle(),orderCancelledQueueUrl);
                logger.info("Successfully deleted event: OrderCancelled with message body = " + message.body());
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse OrderCancelled event with message ID: {}. Error: {}", message.messageId(),e.getMessage(),e);
            } catch (Exception e) {
                logger.error("Failed to process message: {}. Exception: {}", message.body(), e.getMessage(), e);
            }
        });
    }
    private void processInventoryReservedEvent(InventoryReservedEventData eventData) throws JsonProcessingException, UnAvailableAgentException {
        // FInd an agent
        final var agentId = assignAgent(eventData.getCustomerId(), eventData.getReservedItems(), eventData.getOrderId());

        // Publish AgentAssigned event
        publishAgentAssignedEvent(eventData.getCustomerId(), eventData.getReservedItems(), agentId,eventData.getOrderId());
    }

    /**
     * Assigns an available agent to handle a customer's order.
     * This method finds an available agent and marks them as unavailable once assigned.
     * If no agent is available, it triggers a failure event notification.
     *
     * @param customerId The ID of the customer who needs an agent
     * @param orderItems List of items in the order that needs agent assignment
     * @param orderId The unique identifier of the order
     * @return The ID of the assigned agent
     * @throws UnAvailableAgentException When no agents are available for assignment
     */
    private Long assignAgent(Long customerId, List<OrderItemDto> orderItems, String orderId) throws UnAvailableAgentException {
        final var availableAgent = agentRepository.findAvailableAgent()
                .orElseThrow(() -> {
                    try {
                        publishAgentAssignedFailedEvent(customerId, orderItems, orderId);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error publishing AgentAssignedFailedEvent", e);
                    }
                    return new UnAvailableAgentException("No available agent found");
                });

        // Mark the agent as unavailable
        availableAgent.setAvailable(false);
        agentRepository.save(availableAgent); // Save the updated availability status
        return availableAgent.getId();
    }

    /**
     * Publishes an event to notify that an agent has been successfully assigned to an order.
     * This method creates and sends a message to an AWS SQS queue for further processing.
     *
     * @param customerId The ID of the customer who placed the order
     * @param orderItems List of items in the order that the agent is assigned to
     * @param agentId The ID of the agent who has been assigned
     * @param orderId The unique identifier of the order
     * @throws JsonProcessingException If there's an error serializing the event to JSON
     */
    private void publishAgentAssignedEvent(Long customerId, List<OrderItemDto> orderItems, Long agentId,String orderId) throws JsonProcessingException {
        final var agentAssignedEventData = new AgentAssignedEventData(customerId, orderItems, agentId,orderId);
        final var agentAssignedEvent = new AgentAssignedEvent(agentAssignedEventData);

        final var eventMessageBody = objectMapper.writeValueAsString(agentAssignedEvent);
        logger.info("Agent assigned body"+ eventMessageBody);

        final var sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(agentQueueUrl) // Sending it to the agent queue
                .messageBody(eventMessageBody)
                .build();

        sqsClient.sendMessage(sendMsgRequest);
    }

    /**
     * Publishes an event to notify that agent assignment has failed for an order.
     * This method creates and sends a failure event message to an SQS queue for further processing.
     *
     * @param customerId The ID of the customer whose order failed agent assignment
     * @param orderItems List of order items that were part of the failed assignment
     * @param orderId The unique identifier of the order that failed
     * @throws JsonProcessingException If there's an error converting the event to JSON
     */
    private void publishAgentAssignedFailedEvent(Long customerId, List<OrderItemDto> orderItems, String orderId) throws JsonProcessingException {
        final var agentAssignedFailedEventData = new AgentAssignmentFailedEventData(orderId,customerId,orderItems);
        final var agentAssignedFailedEvent = new AgentAssignmentFailedEvent(agentAssignedFailedEventData);

        final var eventMessageBody = objectMapper.writeValueAsString(agentAssignedFailedEvent);
        logger.info("Failed Agent assigned body"+ eventMessageBody);

        final var sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(agentFailureQueueUrl) // Sending it to the agent queue
                .messageBody(eventMessageBody)
                .build();

        sqsClient.sendMessage(sendMsgRequest);
    }

    /**
     * Deletes a processed message from the SQS queue to prevent reprocessing.
     * In SQS, messages must be explicitly deleted after successful processing,
     * otherwise they will become visible again after the visibility timeout period.
     *
     * Process Flow:
     * 1. Message is successfully processed
     * 2. Delete request is sent to SQS using the receipt handle
     * 3. Message is permanently removed from the queue
     *
     * @param receiptHandle The receipt handle of the message to be deleted. This is a unique
     *                     identifier provided by SQS when the message is received, NOT the message ID.
     *                     Receipt handles are temporary and expire based on the visibility timeout.
     * @param queueUrl The URL of the SQS queue from which to delete the message
     *
     * @throws IllegalArgumentException if receiptHandle or queueUrl is null/empty
     */
    private void deleteMessage(String receiptHandle,String queueUrl) {
        final var deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();
        sqsClient.deleteMessage(deleteMessageRequest);
    }

    /**
     * Processes a compensating transaction for an agent by making them available again.
     * This method is typically called when a transaction needs to be reversed or compensated.
     *
     * @param agentId The unique identifier of the agent to process
     * @throws IllegalArgumentException if the agent with the given ID is not found
     */
    public void processCompensatingTransaction(Long agentId) {
        final var agentOptional = agentRepository.findById(agentId);
        agentOptional.ifPresentOrElse(
                agent -> {
                    // Make agent available again
                    agent.setAvailable(true);
                    agentRepository.save(agent);  // Save the updated status
                },
                () -> {
                    // If agent is not found, throw an exception or handle accordingly
                    throw new IllegalArgumentException("Agent with ID " + agentId + " not found.");
                }
        );
    }



}
