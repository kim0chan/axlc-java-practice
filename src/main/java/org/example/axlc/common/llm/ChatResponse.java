package org.example.axlc.common.llm;

import java.util.List;

/**
 * OpenAI API 응답 바디 DTO
 */
public class ChatResponse {
    public List<Choice> choices;

    public static class Choice {
        public ChatMessage message;
    }
}
