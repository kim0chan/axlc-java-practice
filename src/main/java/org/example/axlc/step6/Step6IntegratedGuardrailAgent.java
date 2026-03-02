package org.example.axlc.step6;

import org.example.axlc.common.ConsoleColor;
import org.example.axlc.common.LoadingSpinner;
import org.example.axlc.common.llm.ChatMessage;
import org.example.axlc.common.llm.OpenAiLlmClient;
import org.example.axlc.step3.MeetingService;
import org.example.axlc.step4.tool.FunctionalTool;
import org.example.axlc.step4.tool.ToolRegistry;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * [주제 1 확장: 가드레일 통합 에이전트]
 * Step 4의 Tool Call 에이전트에 가드레일(Step 6)을 통합하여 보안을 강화한 실무형 에이전트입니다.
 * 강의 시나리오: 가드레일 유무에 따른 'Token 악용 공격(Off-topic)' 대응 차이를 보여줍니다.
 */
public class Step6IntegratedGuardrailAgent {
    private static final MeetingService meetingService = new MeetingService();
    private static final ToolRegistry toolRegistry = new ToolRegistry();
    private static final OpenAiLlmClient llmClient = new OpenAiLlmClient();
    private static final Step6Guardrail guardrail = new Step6Guardrail(llmClient);

    // 가드레일 활성화 여부 (강의 데모용)
    private static boolean isGuardrailEnabled = false;

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        // 도구 등록 (Step 4와 동일)
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

        System.out.println(ConsoleColor.CYAN + "=== [Step 6] 가드레일 통합 에이전트 데모 ===" + ConsoleColor.RESET);
        System.out.println("'guard on' 또는 'guard off'을 입력하여 가드레일을 제어하세요.");
        
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", buildSystemPrompt()));

        while (true) {
            String status = isGuardrailEnabled ? ConsoleColor.GREEN + "\uD83D\uDC82 ON" : ConsoleColor.RED + "\uD83D\uDC82 OFF";
            System.out.print(" [" + status + ConsoleColor.RESET + "] " + ConsoleColor.BLUE + "[User]: " + ConsoleColor.RESET);
            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input)) break;

            if ("guard on".equalsIgnoreCase(input)) {
                isGuardrailEnabled = true;
                System.out.println(ConsoleColor.GREEN + ">> \uD83D\uDC82 가드레일이 활성화되었습니다!" + ConsoleColor.RESET);
                continue;
            } else if ("guard off".equalsIgnoreCase(input)) {
                isGuardrailEnabled = false;
                System.out.println(ConsoleColor.RED + ">> \uD83D\uDC82 가드레일이 비활성화되었습니다!" + ConsoleColor.RESET);
                continue;
            }

            // 🌟 가드레일 체크
            if (isGuardrailEnabled) {
                if (!guardrail.checkInput(input)) {
                    System.out.println(ConsoleColor.RED + "[Guardrail System]: " + ConsoleColor.RESET + "죄송합니다. 부적절한 요청은 처리할 수 없습니다.");
                    continue; // 🌟 메인 로직(LLM 호출)을 아예 타지 않음! (Token 절약)
                }
            }

            messages.add(new ChatMessage("user", input));
            processAgentLoop(messages);
        }
    }

    private static void processAgentLoop(List<ChatMessage> messages) throws Exception {
        while (true) {
            ChatMessage response = askLlmWithSpinner(messages, toolRegistry.getToolSpecifications());
            messages.add(response);

            if (response.tool_calls != null && !response.tool_calls.isEmpty()) {
                System.out.println(ConsoleColor.PURPLE + "[Agent Thinking]: " + ConsoleColor.RESET + "도구를 사용해야겠어...");
                for (ChatMessage.ToolCall toolCall : response.tool_calls) {
                    System.out.println(ConsoleColor.YELLOW + ">> Executing Tool: " + ConsoleColor.RESET + toolCall.function.name);
                    String result = toolRegistry.handleToolCall(toolCall);
                    System.out.println(ConsoleColor.CYAN + ">> Result: " + ConsoleColor.RESET + result);
                    messages.add(new ChatMessage("tool", result, toolCall.id, toolCall.function.name));
                }
                continue;
            }

            if (response.content != null && !response.content.isEmpty()) {
                System.out.println(ConsoleColor.GREEN + "[AI]: " + ConsoleColor.RESET + response.content);
            }
            break;
        }
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
        return """
                당신은 스마트 오피스의 미팅 예약 비서입니다.
                회의 예약 관련 업무를 도와주세요. 관련 없는 대화도 평범하게 이어가세요.
                """;
    }
}
