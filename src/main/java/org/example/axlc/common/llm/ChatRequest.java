package org.example.axlc.common.llm;

import java.util.List;

/**
 * OpenAI API 요청 바디 DTO
 */
public class ChatRequest {
    public String model;
    public List<ChatMessage> messages;
    public List<Object> tools; // Tool 사양 리스트
    public String tool_choice;

    public ChatRequest(String model, List<ChatMessage> messages) {
        this.model = model;
        this.messages = messages;
    }

    public ChatRequest(String model, List<ChatMessage> messages, List<Object> tools) {
        this.model = model;
        this.messages = messages;
        this.tools = tools;
        if (tools != null && !tools.isEmpty()) {
            this.tool_choice = "auto";
        }
    }
}
