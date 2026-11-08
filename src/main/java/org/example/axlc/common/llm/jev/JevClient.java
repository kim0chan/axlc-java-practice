package org.example.axlc.common.llm.jev;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.cdimascio.dotenv.Dotenv;
import org.example.axlc.common.llm.jev.JevModels.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TypeSafe AI Jev (System One) Model Client for Java
 */
public class JevClient {
    private final String apiKey;
    private final String endpoint;
    private final HttpClient httpClient;
    private final Gson gson;

    public JevClient() {
        Dotenv dotenv = Dotenv.load();
        this.apiKey = dotenv.get("JEV_API_KEY");
        if (this.apiKey == null) {
            throw new IllegalArgumentException("JEV_API_KEY not found in .env file");
        }
        this.endpoint = "https://api.typesafe.ai/v1/systemone";
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new GsonBuilder().create();
    }

    public JevClient(String apiKey) {
        this.apiKey = apiKey;
        this.endpoint = "https://api.typesafe.ai/v1/systemone";
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new GsonBuilder().create();
    }

    /**
     * Jev 모델에게 판단(Decision)을 요청합니다.
     */
    public JevResponse decide(Object state, Map<String, JevQuestion> questions) {
        return decide(state, questions, "jev-latest");
    }

    public JevResponse decide(Object state, Map<String, JevQuestion> questions, String model) {
        try {
            JevRequest jevRequest = new JevRequest(model, state, questions);
            String requestBody = gson.toJson(jevRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + this.apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Jev API Error (" + response.statusCode() + "): " + response.body());
            }

            return gson.fromJson(response.body(), JevResponse.class);

        } catch (Exception e) {
            throw new RuntimeException("Exception in JevClient: " + e.getMessage(), e);
        }
    }

    // --- 편의 메서드들 ---

    /** 단일 Choice 질문을 던지고 선택된 결과를 반환합니다. */
    public String choice(Object state, String instructions, Map<String, String> criteria) {
        Map<String, JevQuestion> questions = new HashMap<>();
        questions.put("q1", new JevQuestion("choice", instructions, criteria));
        JevResponse res = decide(state, questions);
        return res.answers.get("q1").choice;
    }

    /** 단일 Noul 질문을 던지고 확률값을 반환합니다. */
    public Double noul(Object state, String instructions) {
        Map<String, JevQuestion> questions = new HashMap<>();
        questions.put("q1", new JevQuestion("noul", instructions));
        JevResponse res = decide(state, questions);
        return res.answers.get("q1").noul;
    }

    /** 단일 Score 질문을 던지고 점수를 반환합니다. */
    public Double score(Object state, String instructions, List<String> criteria) {
        Map<String, JevQuestion> questions = new HashMap<>();
        questions.put("q1", new JevQuestion("score", instructions, criteria));
        JevResponse res = decide(state, questions);
        return res.answers.get("q1").score;
    }
}
