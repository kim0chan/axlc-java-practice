package org.example.axlc.step0;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.cdimascio.dotenv.Dotenv;
import org.example.axlc.common.*;
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
import java.util.Scanner;

/**
 * Step 0: First API Call
 * 환경 변수를 설정하고, LLM API를 호출하여 Stateless 특성을 이해하기 위한 실습입니다.
 */
public class Step0StatelessChat {
    // 환경 변수 로딩 (OpenAI API Key)
    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("OPENAI_API_KEY");
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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

        System.out.println(ConsoleColor.CYAN + "=== [Step 0] LLM과 대화를 시작합니다 (종료하려면 'exit' 입력) ===" + ConsoleColor.RESET);

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

            // 2. 요청 바디 구성
            ChatMessage message = new ChatMessage("user", input);
            List<ChatMessage> messages = new ArrayList<>(List.of(message));
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

            // 3. API 호출
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // --- 로딩 애니메이션 중지 ---
            spinner.stop();
            spinnerThread.join();

            if (response.statusCode() != 200) {
                System.err.println("\nAPI 호출 실패 - 상태 코드: " + response.statusCode());
                System.err.println("응답 내용: " + response.body());
                continue;
            }

            // 4. 응답 파싱 후 출력
            ChatResponse chatResponse = gson.fromJson(response.body(), ChatResponse.class);
            String aiResponse = chatResponse.choices.getFirst().message.content;
            
            System.out.println(ConsoleColor.GREEN + "[AI]: " + ConsoleColor.RESET + aiResponse);
        }
    }
}
