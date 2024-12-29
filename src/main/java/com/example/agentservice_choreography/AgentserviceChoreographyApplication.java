package com.example.agentservice_choreography;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgentserviceChoreographyApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgentserviceChoreographyApplication.class, args);
	}

}
