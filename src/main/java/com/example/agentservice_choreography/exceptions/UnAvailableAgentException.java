package com.example.agentservice_choreography.exceptions;

public class UnAvailableAgentException extends Exception{
    private final String msg;

    public UnAvailableAgentException(String message) {
        super(message);
        this.msg = message;
    }
}
