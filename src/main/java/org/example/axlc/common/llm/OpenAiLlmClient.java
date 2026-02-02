package org.example.axlc.common.llm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * OpenAI API 구현체
 * 표준 HTTP Client와 Gson을 사용하여 OpenAI와 통신합니다.
 */
public class OpenAiLlmClient implements LlmClient {
    private final String apiKey;
    private final String apiUrl;
    private final String modelName;
    private final HttpClient httpClient;
    private final Gson gson;

    // 기본 생성자 (환경변수에서 로드)
    public OpenAiLlmClient() {
        Dotenv dotenv = Dotenv.load();
        this.apiKey = dotenv.get("OPENAI_API_KEY");
        if (this.apiKey == null) {
            throw new IllegalArgumentException("OPENAI_API_KEY not found in .env file");
        }
        this.apiUrl = "https://api.openai.com/v1/chat/completions";
        this.modelName = "gpt-5-nano";
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new GsonBuilder().create();
    }

    public OpenAiLlmClient(String apiKey) {
        this(apiKey, "https://api.openai.com/v1/chat/completions", "gpt-5-nano");
    }

    // 전체 커스텀 생성자
    public OpenAiLlmClient(String apiKey, String apiUrl, String modelName) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.modelName = modelName;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new GsonBuilder().create();
    }

    @Override
    public String ask(List<ChatMessage> messages) {
        try {
            // 1. 요청 객체 생성
            ChatRequest chatRequest = new ChatRequest(this.modelName, messages);
            String requestBody = gson.toJson(chatRequest);

            // 2. HTTP Request 구성
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + this.apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // 3. API 호출
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                // 에러 상황 (로그 출력 또는 예외 던지기)
                System.err.println("OpenAI API Error: " + response.statusCode() + " - " + response.body());
                return "Error: " + response.statusCode();
            }

            // 4. 응답 파싱
            ChatResponse chatResponse = gson.fromJson(response.body(), ChatResponse.class);
            if (chatResponse.choices == null || chatResponse.choices.isEmpty()) {
                return "Error: No choices returned.";
            }

            return chatResponse.choices.getFirst().message.content;

        } catch (Exception e) {
            e.printStackTrace();
            return "Exception: " + e.getMessage();
        }
    }
}
