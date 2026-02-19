package org.example.axlc.common.llm;

import java.util.List;

/**
 * 메시지 DTO 객체
 */
public class ChatMessage {
    public String role;
    public String content;
    public String name; // Tool 이름 (tool role일 때 사용)
    public String tool_call_id; // Tool 실행 ID (tool role일 때 사용)
    public List<ToolCall> tool_calls; // Assistant가 Tool 사용을 요청할 때

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    // Tool 실행 결과를 보낼 때 사용하는 생성자
    public ChatMessage(String role, String content, String tool_call_id, String name) {
        this.role = role;
        this.content = content;
        this.tool_call_id = tool_call_id;
        this.name = name;
    }

    /**
     * Tool Call 상세 정보
     */
    public static class ToolCall {
        public String id;
        public String type = "function";
        public Function function;

        public static class Function {
            public String name;
            public String arguments; // JSON String
        }
    }
}
