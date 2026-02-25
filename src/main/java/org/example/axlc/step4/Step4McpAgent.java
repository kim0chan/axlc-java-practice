package org.example.axlc.step4;

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
 * Step 4: MCP Agent
 * MultiMcpManager를 사용하여 로컬 및 원격(SSE) MCP 서버를 통합 관리하는 에이전트입니다.
 */
public class Step4McpAgent {
    private static final OpenAiLlmClient llmClient = new OpenAiLlmClient();
    private static final ToolRegistry toolRegistry = new ToolRegistry();

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        System.out.println(ConsoleColor.CYAN + "=== [Step 4] Refactored MCP Agent ===" + ConsoleColor.RESET);
        System.out.println("로컬 명령어(npx...)를 입력하여 MCP 서버를 추가하세요.");
        System.out.println("예시 1: npx.cmd -y mcp-server-fetch-typescript");
        System.out.println("예시 2: npx.cmd -y @modelcontextprotocol/server-filesystem");
        
        try (MultiMcpManager mcpManager = new MultiMcpManager(toolRegistry)) {
            while (true) {
                System.out.print(ConsoleColor.YELLOW + "\n[Add MCP Server] (Enter to skip or 'run' to start): " + ConsoleColor.RESET);
                String commandInput = scanner.nextLine().trim();
                
                if (commandInput.equalsIgnoreCase("run")) break;
                if (commandInput.isEmpty()) {
                    if (toolRegistry.getToolSpecifications().isEmpty()) {
                        System.out.println("최소 하나 이상의 MCP 서버가 필요합니다.");
                        continue;
                    }
                    break;
                }

                try {
                    if (commandInput.startsWith("http")) {
                        mcpManager.addMcpServer(commandInput);
                    } else {
                        mcpManager.addMcpServer(commandInput.split(" "));
                    }
                    System.out.println(ConsoleColor.GREEN + "✅ 서버 연결 성공! 현재 도구 수: " + toolRegistry.getToolSpecifications().size() + ConsoleColor.RESET);
                } catch (Exception e) {
                    System.err.println("❌ 연결 실패: " + e.getMessage());
                }
            }

            System.out.println(ConsoleColor.CYAN + "\n>> 에이전트가 준비되었습니다. 대화를 시작하세요! (exit 입력 시 종료)" + ConsoleColor.RESET);

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", 
                "당신은 MCP 도구를 사용하는 유능한 에이전트입니다. " +
                "사용 가능한 도구들을 활용하여 사용자의 요청을 최선을 다해 해결하세요."));

            while (true) {
                System.out.print(ConsoleColor.BLUE + "\n[User]: " + ConsoleColor.RESET);
                String input = scanner.nextLine();

                if ("exit".equalsIgnoreCase(input)) break;

                messages.add(new ChatMessage("user", input));
                processAgentLoop(messages);
            }
        }
    }

    private static void processAgentLoop(List<ChatMessage> messages) throws Exception {
        int maxIterations = 100;

        for (int i = 0; i < maxIterations; i++) {
            ChatMessage response = askLlmWithSpinner(messages, toolRegistry.getToolSpecifications());
            messages.add(response);

            if (response.tool_calls != null && !response.tool_calls.isEmpty()) {
                for (ChatMessage.ToolCall toolCall : response.tool_calls) {
                    System.out.println(ConsoleColor.PURPLE + "[Thinking]: " + ConsoleColor.RESET + "도구 '" + toolCall.function.name + "' 실행 중...");
                    System.out.println(ConsoleColor.BLACK_BRIGHT + "\t args: " + toolCall.function.arguments + ConsoleColor.RESET);
                    
                    String result = toolRegistry.handleToolCall(toolCall);
                    System.out.println(ConsoleColor.BLACK_BRIGHT + "\tAdded tool call result (" + result.length() + " characters) into context." + ConsoleColor.RESET);

                    messages.add(new ChatMessage("tool", result, toolCall.id, toolCall.function.name));
                }
                continue;
            }

            if (response.content != null && !response.content.isEmpty()) {
                System.out.println(ConsoleColor.GREEN + "\n[AI]: " + ConsoleColor.RESET + response.content);
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
}
