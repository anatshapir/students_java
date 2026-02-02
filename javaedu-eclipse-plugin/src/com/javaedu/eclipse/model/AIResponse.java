package com.javaedu.eclipse.model;

/**
 * Model representing an AI helper response.
 */
public class AIResponse {
    private String response;
    private int remainingRequests;

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public int getRemainingRequests() {
        return remainingRequests;
    }

    public void setRemainingRequests(int remainingRequests) {
        this.remainingRequests = remainingRequests;
    }
}
