package com.example.aimanager.dto;

import java.util.List;

public class CompareChatResponse {
    private Long sessionId;
    private List<CompareResultItem> results;
    private String userMessage;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public List<CompareResultItem> getResults() { return results; }
    public void setResults(List<CompareResultItem> results) { this.results = results; }
    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
}
