package org.example.axlc.step4;

import org.example.axlc.common.ConsoleColor;
import org.example.axlc.common.LoadingSpinner;
import org.example.axlc.common.llm.ChatMessage;
import org.example.axlc.common.llm.OpenAiLlmClient;
import org.example.axlc.step3.MeetingService;
import org.example.axlc.step4.tool.FunctionalTool;
import org.example.axlc.step4.tool.ToolRegistry;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Step 4: Tool Calling AI Agent
 * LLM에게 도구(Tool)를 제공하고, 필요시 스스로 도구를 사용하도록 하는 자율형 에이전트입니다.
 */
public class Step4ToolCallAgent {
    private static final MeetingService meetingService = new MeetingService();
    private static final ToolRegistry toolRegistry = new ToolRegistry();
    private static final OpenAiLlmClient llmClient = new OpenAiLlmClient();

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        // 🌟 1. 도구 등록 (CRUD 풀세트)
        toolRegistry.register(new FunctionalTool(
            meetingService, "createMeeting", "create_meeting", "새로운 미팅 예약을 생성합니다."
        ));
        
        toolRegistry.register(new FunctionalTool(
            meetingService, "updateMeeting", "update_meeting", "기존 미팅 예약을 수정합니다. 예약 ID가 필수입니다. ID를 모르면 목록을 먼저 조회하세요."
        ));
        
        toolRegistry.register(new FunctionalTool(
            meetingService, "deleteMeeting", "delete_meeting", "미팅 예약을 취소합니다. 예약 ID가 필수입니다."
        ));
        
        toolRegistry.register(new FunctionalTool(
            meetingService, "getMeetingList", "list_meetings", "현재 예약된 모든 미팅 목록을 조회합니다."
        ));

        System.out.println(ConsoleColor.CYAN + "=== [Step 4] Tool Call 에이전트: 스마트 비서 v2 ===" + ConsoleColor.RESET);
        System.out.println("목표: LLM에게 직접 도구를 쥐어주고 스스로 문제를 해결하게 합니다.");
        printMeetingList(); // 초기 예약 현황 출력

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", buildSystemPrompt()));

        while (true) {
            System.out.print(ConsoleColor.BLUE + "[User]: " + ConsoleColor.RESET);
            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input)) break;

            messages.add(new ChatMessage("user", input));

            // AI Agent Loop 시작
            processAgentLoop(messages);
        }
    }

    /**
     * AI Agent Loop
     * Tool Call이 없을 때까지 LLM과 대화하며 도구를 실행합니다.
     */
    private static void processAgentLoop(List<ChatMessage> messages) throws Exception {
        while (true) {
            // 1. LLM에게 현재 상황 질문 (도구 명세 포함)
            ChatMessage response = askLlmWithSpinner(messages, toolRegistry.getToolSpecifications());
            
            // 대화 히스토리에 AI의 응답(Assistant Message) 추가
            messages.add(response);

            // 🌟 Case A: LLM이 도구 사용을 요청함 (Tool Call)
            if (response.tool_calls != null && !response.tool_calls.isEmpty()) {
                System.out.println(ConsoleColor.PURPLE + "[Agent Thinking]: " + ConsoleColor.RESET + "도구를 사용해야겠어...");

                for (ChatMessage.ToolCall toolCall : response.tool_calls) {
                    System.out.println(ConsoleColor.YELLOW + ">> Executing Tool: " + ConsoleColor.RESET + toolCall.function.name);
                    System.out.println("   Args: " + toolCall.function.arguments);

                    // 2. 실제 도구 실행 (ToolRegistry 활용)
                    String result = toolRegistry.handleToolCall(toolCall);
                    System.out.println(ConsoleColor.CYAN + ">> Result: " + ConsoleColor.RESET + result);

                    // 예약 성공 시 목록 다시 출력
                    if (result.contains("SUCCESS")) {
                        printMeetingList();
                    }

                    // 3. 실행 결과를 다시 대화 히스토리에 추가 (role="tool")
                    messages.add(new ChatMessage("tool", result, toolCall.id, toolCall.function.name));
                }

                // 도구 실행 결과를 바탕으로 LLM에게 다시 물어봄 (Next Loop)
                continue;
            }

            // 🌟 Case B: 최종 응답 (사용자에게 출력)
            if (response.content != null && !response.content.isEmpty()) {
                System.out.println(ConsoleColor.GREEN + "[AI]: " + ConsoleColor.RESET + response.content);
            }
            break; // 루프 종료
        }
    }

    private static void printMeetingList() {
        List<String> meetings = meetingService.findAllMeetings();
        System.out.println(ConsoleColor.CYAN + "--------------------------------------------------");
        System.out.println("📅 현재 예약된 미팅 목록 (Total: " + meetings.size() + ")");
        if (meetings.isEmpty()) {
            System.out.println("   (예약 없음)");
        } else {
            for (int i = 0; i < meetings.size(); i++) {
                System.out.println("   " + (i + 1) + ". " + meetings.get(i));
            }
        }
        System.out.println("--------------------------------------------------" + ConsoleColor.RESET);
    }

    private static ChatMessage askLlmWithSpinner(List<ChatMessage> messages, List<Object> tools) throws Exception {
        LoadingSpinner spinner = new LoadingSpinner();
        Thread thread = new Thread(spinner);
        thread.start();

        ChatMessage response = llmClient.askWithTools(messages, tools);

        spinner.stop();
        thread.join();
        return response;
    }

    private static String buildSystemPrompt() {
        LocalDate today = LocalDate.now();
        String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
        String todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return """
                당신은 스마트 오피스의 미팅 예약 비서입니다.
                현재 시각은 %s %s입니다.
                
                # 가이드라인
                - 사용자가 예약을 요청하거나, 변경/취소를 원할 때 제공된 도구를 사용하세요.
                - **수정이나 취소 시 예약 번호(ID)를 모른다면, 반드시 `list_meetings` 도구를 먼저 호출하여 ID를 확인하세요.**
                - 예약 가능 여부를 묻는다면 목록을 조회하여 판단하세요.
                - 도구 실행 결과(SUCCESS/ERROR)를 보고 사용자에게 최종 결과를 친절하게 안내하세요.
                - 예약과 관련 없는 대화는 평범하게 이어가세요.
                """.formatted(todayStr, dayOfWeek);
    }
}
