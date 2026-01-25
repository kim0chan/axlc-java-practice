package org.example.axlc.step1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.cdimascio.dotenv.Dotenv;
import org.example.axlc.common.*;
import org.example.axlc.common.llm.ChatMessage;
import org.example.axlc.common.llm.ChatRequest;
import org.example.axlc.common.llm.ChatResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.io.PrintStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Step 1: Basic Context Management
 * LLM의 Stateless 특성을 이해하고, List를 이용해 직접 대화 맥락을 관리하는 기초 실습입니다.
 */
public class Step1ContextChat {
    // 환경 변수 로딩 (OpenAI API Key)
    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("OPENAI_API_KEY");
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // 🌟 대화 히스토리를 저장할 리스트 (메모리 내 저장)
    private static final List<ChatMessage> messages = new ArrayList<>();

    private static void addMessage(String role, String content) {
        messages.add(new ChatMessage(role, content));
    }

    public static void main(String[] args) throws Exception {
        // 출력을 UTF-8로 설정
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        // 환경 변수 체크
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.equals("your_key_here")) {
            System.err.println("Error: .env 파일에 OPENAI_API_KEY를 설정해주세요!");
            return;
        }

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        HttpClient client = HttpClient.newHttpClient();

        // 🌟 TODO 1: 시스템 메시지를 추가하여 AI에게 페르소나를 부여해보세요.
        // 예: "당신은 친절한 Java 코칭 전문가입니다."
        // addMessage("system", "YOUR_PERSONA_HERE");

        System.out.println(ConsoleColor.CYAN + "=== [Step 1] LLM과 대화를 시작합니다 (종료하려면 'exit' 입력) ===" + ConsoleColor.RESET);

        // Core Chatting Loop
        while (true) {
            // 사용자 입력 받기
            System.out.print(ConsoleColor.BLUE + "[User]: " + ConsoleColor.RESET);
            String input = scanner.nextLine();

            // Escape 조건
            if ("exit".equalsIgnoreCase(input)) {
                System.out.println(ConsoleColor.CYAN + "대화를 종료합니다. Bye!" + ConsoleColor.RESET);
                break;
            }

            // 🌟 2. 사용자 메시지 추가
            addMessage("user", input);

            // 🌟 TODO 2: 대화가 너무 길어지면 토큰 사용량이 늘어납니다.
            // 최근 N개의 메시지만 유지하도록 messages 리스트를 관리하는 로직을 추가해보세요!

            // 3. 요청 바디 구성
            ChatRequest chatRequest = new ChatRequest("gpt-5-nano", messages);

            // LLM API Request 준비
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(chatRequest)))
                    .build();

            // --- 로딩 애니메이션 시작 ---
            LoadingSpinner spinner = new LoadingSpinner();
            Thread spinnerThread = new Thread(spinner);
            spinnerThread.start();

            // 4. API 호출
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // --- 로딩 애니메이션 중지 ---
            spinner.stop();
            spinnerThread.join();

            if (response.statusCode() != 200) {
                System.err.println("\nAPI 호출 실패 - 상태 코드: " + response.statusCode());
                System.err.println("응답 내용: " + response.body());
                continue;
            }

            // 5. 응답 파싱 후 출력
            ChatResponse chatResponse = gson.fromJson(response.body(), ChatResponse.class);
            String aiResponse = chatResponse.choices.getFirst().message.content;

            System.out.println(ConsoleColor.GREEN + "[AI]: " + ConsoleColor.RESET + aiResponse);

            // 🌟 6. AI의 답변도 히스토리에 추가
            addMessage("assistant", aiResponse);
        }
    }
}
