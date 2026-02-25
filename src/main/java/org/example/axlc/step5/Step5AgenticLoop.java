package org.example.axlc.step5;

import org.example.axlc.common.ConsoleColor;
import org.example.axlc.common.LoadingSpinner;
import org.example.axlc.common.llm.ChatMessage;
import org.example.axlc.common.llm.OpenAiLlmClient;
import org.example.axlc.step4.mcp.MultiMcpManager;
import org.example.axlc.step4.tool.ToolRegistry;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Step 5: Agentic Loop (with MCP & ReAct)
 * MCP 도구들을 활용하여 실제 세상과 상호작용하며 문제를 해결하는 자율 에이전트입니다.
 */
public class Step5AgenticLoop {
    private static final OpenAiLlmClient llmClient = new OpenAiLlmClient();
    private static final ToolRegistry toolRegistry = new ToolRegistry();

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        System.out.println(ConsoleColor.CYAN + "=== [Step 5] Agentic Loop (feat. MCP) ===" + ConsoleColor.RESET);
        
        try (MultiMcpManager mcpManager = new MultiMcpManager(toolRegistry)) {
            // 🌟 1. 원하는 MCP 서버들을 추가합니다.
            System.out.println(ConsoleColor.YELLOW + "서버를 초기화 중입니다. 잠시만 기다려주세요..." + ConsoleColor.RESET);
            
            String npx = System.getProperty("os.name").toLowerCase().contains("win") ? "npx.cmd" : "npx";
            
            // 웹 컨텐츠 Fetch 서버 (NPM 버전)
            mcpManager.addMcpServer(npx, "-y", "@modelcontextprotocol/server-filesystem", "D:\\repositories\\axlc-java-practice");

            System.out.println(ConsoleColor.GREEN + "✅ 모든 MCP 서버가 준비되었습니다!\n" + ConsoleColor.RESET);

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", buildSystemPrompt()));

            while (true) {
                System.out.print(ConsoleColor.BLUE + "[User]: " + ConsoleColor.RESET);
                String input = scanner.nextLine();

                if ("exit".equalsIgnoreCase(input)) break;

                messages.add(new ChatMessage("user", input));

                // ReAct Agentic Loop 시작
                runReActLoop(messages);
            }
        }
    }

    private static void runReActLoop(List<ChatMessage> messages) throws Exception {
        int maxIterations = 100;

        for (int i = 0; i < maxIterations; i++) {
            System.out.println(ConsoleColor.BLACK_BRIGHT + "\n[Iteration " + (i + 1) + "]" + ConsoleColor.RESET);

            // 1. LLM 호출
            ChatMessage response = askLlmWithSpinner(messages, toolRegistry.getToolSpecifications());
            messages.add(response);

            // 🌟 2. Thought 추출 및 출력
            if (response.content != null && !response.content.isEmpty()) {
                String content = response.content;
                if (content.contains("Thought:")) {
                    String thought = extractThought(content);
                    System.out.println(ConsoleColor.PURPLE + "Thought: " + ConsoleColor.RESET + thought);
                } else if (response.tool_calls == null) {
                    // Thought가 없고 Tool Call도 없으면 이게 최종 답변
                    System.out.println(ConsoleColor.GREEN + "\n최종 답변: " + ConsoleColor.RESET + content);
                    break;
                }
            }

            // 🌟 3. Action (Tool Call) 처리
            if (response.tool_calls != null && !response.tool_calls.isEmpty()) {
                for (ChatMessage.ToolCall toolCall : response.tool_calls) {
                    System.out.println(ConsoleColor.YELLOW + "Action: " + ConsoleColor.RESET + toolCall.function.name);
                    System.out.println(ConsoleColor.BLACK_BRIGHT + "\t Args: " + toolCall.function.arguments + ConsoleColor.RESET);

                    // 도구 실행
                    String observation = toolRegistry.handleToolCall(toolCall);
                    
                    // Observation 요약 출력
                    String displayObs = observation.length() > 150 ? observation.substring(0, 150) + "... (생략)" : observation;
                    System.out.println(ConsoleColor.CYAN + "Observation: " + ConsoleColor.RESET + displayObs);

                    // 실행 결과를 컨텍스트에 추가
                    messages.add(new ChatMessage("tool", observation, toolCall.id, toolCall.function.name));
                }
                // 도구를 썼으니 다음 루프로 넘어가서 AI가 Observation을 보고 Thought하게 함
            } else {
                // Tool Call이 없는데 위에서 최종 답변 처리가 안 됐다면 (Thought만 있는 경우 등)
                // 만약 내용이 있다면 출력하고 종료
                if (response.content != null && !response.content.isEmpty() && !response.content.contains("Final Answer:")) {
                     System.out.println(ConsoleColor.GREEN + "\n최종 답변: " + ConsoleColor.RESET + response.content);
                     break;
                }
            }
        }
    }

    private static String extractThought(String content) {
        if (content.contains("Thought:")) {
            int start = content.indexOf("Thought:") + 8;
            int end = content.indexOf("Action:");
            if (end == -1) end = content.indexOf("Final Answer:");
            if (end == -1) end = content.length();
            return content.substring(start, end).trim();
        }
        return content.trim();
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
                당신은 MCP(Model Context Protocol) 도구들을 능숙하게 사용하는 만능 AI 에이전트입니다.
                사용자의 복잡한 요청을 해결하기 위해 ReAct(Reasoning and Acting) 패턴을 사용하여 문제를 해결하세요.
                
                # ReAct 패턴 단계:
                1. **Thought**: 현재 상황을 분석하고 어떤 도구가 필요한지, 왜 필요한지 생각합니다.
                    - 이 경우 스스로의 판단 근거를 간결하게 작성하세요.
                2. **Action**: 사용할 도구를 선택하고 실행합니다. (도구 호출 기능을 사용하세요)
                3. **Observation**: 도구 실행 결과를 꼼꼼히 확인합니다.
                4. **Repeat**: 필요하다면 1~3단계를 반복하여 충분한 정보를 모읍니다.
                5. **Final Answer**: 모든 정보를 종합하여 사용자에게 최종 답변을 제공합니다.
                
                # 주의사항:
                - 매 단계마다 반드시 'Thought:'를 적어 당신의 사고 과정을 사용자에게 보여주세요.
                - 도구 실행 결과(Observation)가 예상과 다르다면 당황하지 말고 다른 방법을 찾으세요.
                
                당신은 이제 단순한 챗봇이 아니라, 진짜 세상과 연결된 강력한 에이전트입니다!
                """;
    }
}
