package com.example.agentservice_choreography.repository;

import com.example.agentservice_choreography.model.DeliveryAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<DeliveryAgent, Long> {

    // Custom query to find the first available agent
    @Query("SELECT a FROM DeliveryAgent a WHERE a.available = true ORDER BY a.id ASC LIMIT 1")
    Optional<DeliveryAgent> findAvailableAgent();
}
