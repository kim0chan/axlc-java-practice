package org.example.axlc.step4.tool;

import org.example.axlc.step4.mcp.McpClient;
import java.util.Map;

/**
 * MCP 서버에서 제공하는 도구를 위한 Tool 구현체
 */
public class McpTool implements Tool {
    private final McpClient mcpClient;
    private final String name;
    private final String description;
    private final Map<String, Object> parametersSchema;

    public McpTool(McpClient mcpClient, String name, String description, Map<String, Object> parametersSchema) {
        this.mcpClient = mcpClient;
        this.name = name;
        this.description = description;
        this.parametersSchema = parametersSchema;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return parametersSchema;
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        try {
            return mcpClient.callTool(name, arguments);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
