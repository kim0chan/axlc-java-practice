package org.example.axlc.step2;

import org.example.axlc.common.llm.LlmClient;
import org.example.axlc.common.llm.OpenAiLlmClient;

public class Step2Main {
    public static void main(String[] args) throws Exception {
        // 1. LLM Client 초기화
        LlmClient llmClient = new OpenAiLlmClient();

        // 2. 인덱싱 (최초 1회, 이미 되어 있으면 스킵)
        Indexer indexer = new Indexer(llmClient);
        indexer.runIndexing();

        // 3. 챗봇 실행
        AgenticRagChat chat = new AgenticRagChat(llmClient);
        chat.start();
    }
}
