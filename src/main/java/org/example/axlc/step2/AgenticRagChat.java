package org.example.axlc.step2;

import org.example.axlc.common.ConsoleColor;
import org.example.axlc.common.llm.ChatMessage;
import org.example.axlc.common.llm.LlmClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class AgenticRagChat {
    private final LlmClient llmClient;
    private final List<ChatMessage> history = new ArrayList<>();
    private static final String KB_DIR = "src/main/resources/data/knowledge-base";

    public AgenticRagChat(LlmClient llmClient) {
        this.llmClient = llmClient;
        this.history.add(new ChatMessage("system", "너는 AI 응용 개발 전문 튜터야. 지식 베이스의 내용을 바탕으로 답변해줘."));
    }

    public void start() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println(ConsoleColor.CYAN + ">>> Agentic RAG Chatbot 시작! (exit 입력 시 종료)" + ConsoleColor.RESET);

        while (true) {
            System.out.print(ConsoleColor.BLUE + "[User]: " + ConsoleColor.RESET);
            String userInput = scanner.nextLine();
            if ("exit".equalsIgnoreCase(userInput)) break;

            // 1. Planner/Router: 어떤 문서를 읽을지 결정
            String selectedFile = selectDocument(userInput);
            String context = "";

            if (!"NONE".equalsIgnoreCase(selectedFile) && Files.exists(Paths.get(KB_DIR, selectedFile))) {
                System.out.println(ConsoleColor.YELLOW + ">>> 지식 탐색 중... [" + selectedFile + "] 파일을 읽습니다." + ConsoleColor.RESET);
                context = Files.readString(Paths.get(KB_DIR, selectedFile));
            } else {
                System.out.println(ConsoleColor.YELLOW + ">>> 관련 지식을 찾지 못했습니다(또는 파일 없음). 일반 답변을 생성합니다." + ConsoleColor.RESET);
            }

            // 2. Generator: 문맥 주입 후 답변 생성
            String finalPrompt;
            if (context.isEmpty()) {
                finalPrompt = userInput;
            } else {
                finalPrompt = "다음 문맥을 참고하여 답변해줘.\n\n[문맥]\n" + context + "\n\n[질문]\n" + userInput;
            }
            
            history.add(new ChatMessage("user", finalPrompt));
            String response = llmClient.ask(history);
            
            System.out.println(ConsoleColor.GREEN + "[AI]: " + response + ConsoleColor.RESET);
            history.add(new ChatMessage("assistant", response));
        }
    }

    private String selectDocument(String query) throws IOException {
        Path kbPath = Paths.get(KB_DIR);
        if (!Files.exists(kbPath)) {
            return "NONE";
        }

        List<String> fileNames = Files.list(kbPath)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());

        if (fileNames.isEmpty()) {
            return "NONE";
        }

        String prompt = "다음은 지식 베이스에 있는 파일 목록입니다:\n" + fileNames + "\n\n" +
                "사용자의 질문 [" + query + "]에 답하기 위해 읽어야 할 파일 하나를 골라주세요.\n" +
                "관련 있는 파일이 없다면 'NONE'이라고만 답하세요.\n" +
                "파일이 있다면 파일명만 출력하세요. (예: pricing.txt)";

        // 주의: 히스토리 없이 단발성 질문(Stateless)으로 처리
        return llmClient.ask(List.of(new ChatMessage("user", prompt))).trim();
    }
}
