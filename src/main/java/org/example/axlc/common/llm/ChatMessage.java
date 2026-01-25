package org.example.axlc.common.llm;

/**
 * 메시지 DTO 객체
 */
public class ChatMessage {
    public String role;
    public String content;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
