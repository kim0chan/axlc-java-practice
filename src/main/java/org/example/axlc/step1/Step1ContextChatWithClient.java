package org.example.axlc.step1;

import org.example.axlc.common.ConsoleColor;
import org.example.axlc.common.LoadingSpinner;
import org.example.axlc.common.llm.ChatMessage;
import org.example.axlc.common.llm.LlmClient;
import org.example.axlc.common.llm.OpenAiLlmClient;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Step 1-3: Chat with Abstraction Layer
 * LlmClient 인터페이스를 사용하여 구체적인 구현(OpenAI, Bedrock)과 비즈니스 로직을 분리합니다.
 */
public class Step1ContextChatWithClient {

    public static void main(String[] args) throws Exception {
        // UTF-8 설정
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        // 1. LLM Client 선택 (원하는 구현체의 주석을 해제하세요.)
        
        // [Option A] OpenAI Client 사용
        LlmClient llmClient = new OpenAiLlmClient();

        // [Option B] Bedrock Client 사용 (사내망 등 OpenAI 차단 환경)
//        LlmClient llmClient = new BedrockLlmClient();
        
        System.out.println(ConsoleColor.CYAN + "=== [Step 1-3] 추상화된 LLM Client와 대화하기 ===" + ConsoleColor.RESET);
        System.out.println("Client Type: " + llmClient.getClass().getSimpleName());

        // 대화 히스토리
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", "당신은 유능한 AI 어시스턴트입니다. 항상 친절하고 정확하게 답변하세요."));

        while (true) {
            System.out.print(ConsoleColor.BLUE + "[User]: " + ConsoleColor.RESET);
            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("Bye!");
                break;
            }

            messages.add(new ChatMessage("user", input));

            // --- 로딩 애니메이션 시작 ---
            LoadingSpinner spinner = new LoadingSpinner();
            Thread spinnerThread = new Thread(spinner);
            spinnerThread.start();

            // 2. Client에게 질문
            String response = llmClient.ask(messages);

            // --- 로딩 애니메이션 중지 ---
            spinner.stop();
            spinnerThread.join();

            System.out.println(ConsoleColor.GREEN + "[AI]: " + ConsoleColor.RESET + response);
            messages.add(new ChatMessage("assistant", response));
        }
    }
}
