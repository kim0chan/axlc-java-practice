package org.example.axlc.common.llm;

import java.util.List;

/**
 * LLM Client Common Interface
 * 특정 벤더(OpenAI, Bedrock, Gemini 등)에 종속되지 않고
 * 공통된 방식으로 대화를 요청하기 위한 인터페이스입니다.
 */
public interface LlmClient {
    /**
     * 대화 메시지 목록을 보내고, LLM의 응답(텍스트)을 반환합니다.
     * @param messages 대화 히스토리 (System, User, Assistant)
     * @return LLM의 응답 내용 (Content)
     */
    String ask(List<ChatMessage> messages);
}
