package org.example.axlc.common.llm;

import java.util.List;

/**
 * OpenAI API 요청 바디 DTO
 */
public class ChatRequest {
    public String model;
    public List<ChatMessage> messages;

    public ChatRequest(String model, List<ChatMessage> messages) {
        this.model = model;
        this.messages = messages;
    }
}
