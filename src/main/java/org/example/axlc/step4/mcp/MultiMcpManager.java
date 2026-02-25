package org.example.axlc.step4.mcp;

import org.example.axlc.step4.tool.McpTool;
import org.example.axlc.step4.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * 여러 개의 MCP 서버를 통합 관리하는 매니저
 */
public class MultiMcpManager implements AutoCloseable {
    private final List<AutoCloseable> clients = new ArrayList<>();
    private final ToolRegistry toolRegistry;

    public MultiMcpManager(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 새로운 MCP 서버를 추가하고 도구들을 등록한다.
     * @param command MCP 서버 실행 명령어(npx...) 또는 Remote URL(https://...)
     */
    public void addMcpServer(String... command) throws Exception {
        if (command.length == 1 && command[0].startsWith("http")) {
            // 원격 SSE 방식
            System.out.println("[MultiMcpManager] Connecting to Remote MCP Server: " + command[0]);
            SseMcpClient client = new SseMcpClient(command[0]);
            clients.add(client);

            List<McpTool> tools = client.getMcpTools();
            for (McpTool tool : tools) {
                toolRegistry.register(tool);
            }
        } else {
            // 로컬 Stdio 방식
            System.out.println("[MultiMcpManager] Adding Local MCP Server: " + String.join(" ", command));
            McpClient client = new McpClient(command);
            clients.add(client);

            List<McpTool> tools = client.getMcpTools();
            for (McpTool tool : tools) {
                toolRegistry.register(tool);
            }
        }
    }

    @Override
    public void close() {
        for (AutoCloseable client : clients) {
            try {
                client.close();
            } catch (Exception e) {
                System.err.println("Error closing client: " + e.getMessage());
            }
        }
    }
}
