package org.example.axlc.step1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.cdimascio.dotenv.Dotenv;
import org.example.axlc.common.ConsoleColor;
import org.example.axlc.common.llm.ChatMessage;
import org.example.axlc.common.llm.ChatRequest;
import org.example.axlc.common.llm.ChatResponse;

import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Step 1-2: Stateless Review Classifier
 * Context를 유지하지 않고 매번 독립적인 요청을 보내는 패턴을 실습합니다.
 * LLM을 단순 챗봇이 아닌, 데이터 분류기(Classifier)로 활용하는 예제입니다.
 */
public class Step1ReviewClassifier {
    // 환경 변수 로딩
    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("OPENAI_API_KEY");
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final Gson gson = new GsonBuilder().create();
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        // 출력을 UTF-8로 설정
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        
        System.out.println(ConsoleColor.CYAN + "=== [Step 1-2] 영화 리뷰 감성 분류기를 시작합니다 ===" + ConsoleColor.RESET);

        // 1. 분류할 영화 리뷰 데이터 준비
        List<String> reviews = List.of(
                "진짜 인생 영화입니다... 보는 내내 눈물이 멈추지 않았어요. 😭",
                "돈 아까워요. 개연성도 없고 연기도 형편없네요. 비추입니다.",
                "영상미는 좋은데 스토리가 너무 뻔해요. 그냥 쏘쏘.",
                "배우들 연기력이 다했네요. 긴장감 넘쳐서 숨도 못 쉬고 봤습니다! 👍",
                "아... 내 아까운 2시간... 감독님 반성하세요.",
                "올해 본 영화 중 최고! 음악이랑 연출이 너무 환상적이에요.",
                "기대 안 하고 봤는데 의외로 꿀잼! 친구랑 가볍게 보기 좋아요.",
                "지루해서 중간에 잤습니다. 돈 주고 보기엔 좀 아깝네요.",
                "마지막 반전이 진짜 대박... 아직도 소름 돋아요. 꼭 보세요!",
                "원작 파괴 수준이네요. 원작 팬으로서 너무 실망스럽습니다."
        );

        int positiveCount = 0;
        int negativeCount = 0;
        int errorCount = 0;

        // 2. 리뷰 데이터 순회
        for (int i = 0; i < reviews.size(); i++) {
            String review = reviews.get(i);
            System.out.printf("Review #%d: \"%s\" -> ", i + 1, review);

            // 3. LLM에게 분류 요청 (Stateless)
            String sentiment = classifyReview(review);

            // 4. 결과 집계
            if (sentiment.contains("긍정적")) {
                System.out.println(ConsoleColor.GREEN + "긍정적" + ConsoleColor.RESET);
                positiveCount++;
            } else if (sentiment.contains("부정적")) {
                System.out.println(ConsoleColor.RED + "부정적" + ConsoleColor.RESET);
                negativeCount++;
            } else {
                System.out.println(ConsoleColor.YELLOW + "판단 불가 (" + sentiment + ")" + ConsoleColor.RESET);
                errorCount++;
            }
            
            // API Rate Limit 방지를 위해 살짝 대기 (선택 사항)
            Thread.sleep(500);
        }

        System.out.println("\n" + ConsoleColor.CYAN + "=== 최종 집계 결과 ===" + ConsoleColor.RESET);
        System.out.printf("총 리뷰 수: %d개\n", reviews.size());
        System.out.printf("긍정 리뷰: %d개\n", positiveCount);
        System.out.printf("부정 리뷰: %d개\n", negativeCount);
        System.out.printf("판단 불가: %d개\n", errorCount);
    }

    /**
     * 단일 리뷰 텍스트를 입력받아 감성을 분류합니다.
     * 이 함수는 이전 호출의 Context(기억)를 유지하지 않습니다. (Stateless)
     */
    private static String classifyReview(String reviewText) {
        try {
            // 🌟 핵심: 매 요청마다 새로운 messages 리스트 생성!
            List<ChatMessage> messages = new ArrayList<>();
            
            // System Prompt: 페르소나 및 출력 형식 강제
            messages.add(new ChatMessage("system", 
                    "너는 영화 리뷰 감성 분류기야. " +
                    "입력된 리뷰를 분석해서 긍정적이면 '긍정적', 부정적이면 '부정적'이라고 딱 3글자로만 대답해. " +
                    "다른 미사여구는 절대 붙이지 마."));
            
            // User Message: 실제 리뷰 데이터
            messages.add(new ChatMessage("user", reviewText));

            // Request 생성 (GPT-4o-mini 등 빠르고 저렴한 모델 추천)
            ChatRequest chatRequest = new ChatRequest("gpt-5-nano", messages);
            // chatRequest.temperature = 0.0; // 분류 작업이므로 일관성을 위해 0에 가깝게 설정하면 좋음 (필드 접근 제어자에 따라 다름)

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(chatRequest)))
                    .build();

            // API 호출
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ChatResponse chatResponse = gson.fromJson(response.body(), ChatResponse.class);
                return chatResponse.choices.getFirst().message.content.trim();
            } else {
                return "Error: " + response.statusCode();
            }

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
