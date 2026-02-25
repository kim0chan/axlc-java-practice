package org.example.axlc.step4.tool;

import org.example.axlc.common.llm.ChatMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 도구 관리자 (Tool Registry)
 * 등록된 도구들을 관리하고, LLM에게 전달할 광고(Advertise)용 명세를 생성합니다.
 */
public class ToolRegistry {
    private final Map<String, Tool> tools = new HashMap<>();

    /**
     * 새로운 도구를 등록합니다.
     */
    public void register(Tool tool) {
        System.out.println("[ToolRegistry] Registering tool: " + tool.getName());
        tools.put(tool.getName(), tool);
    }

    /**
     * 이름으로 도구를 조회합니다.
     */
    public Tool get(String name) {
        return tools.get(name);
    }

    /**
     * LLM 요청에 포함할 도구 명세 리스트를 반환합니다. (Advertisement 역할)
     */
    public List<Object> getToolSpecifications() {
        List<Object> specs = new ArrayList<>();
        for (Tool tool : tools.values()) {
            Map<String, Object> spec = new HashMap<>();
            spec.put("type", "function");
            
            Map<String, Object> function = new HashMap<>();
            function.put("name", tool.getName());
            function.put("description", tool.getDescription());
            Map<String, Object> params = tool.getParametersSchema();
            Map<String, Object> safeParams = new HashMap<>();
            if (params != null) {
                safeParams.putAll(params);
            }

            // OpenAI strictly requires the top-level parameters to be an 'object'.
            // If the MCP server sent something else (like null or "None"), we fix it here.
            if (!"object".equals(safeParams.get("type"))) {
                safeParams.put("type", "object");
            }
            if (!safeParams.containsKey("properties")) {
                safeParams.put("properties", Map.of());
            }
            function.put("parameters", safeParams);
            
            spec.put("function", function);
            specs.add(spec);
        }
        return specs;
    }

    /**
     * LLM의 Tool Call 요청을 처리하고 결과를 반환합니다.
     */
    public String handleToolCall(ChatMessage.ToolCall toolCall) {
        Tool tool = get(toolCall.function.name);
        if (tool == null) {
            return "Error: Tool not found - " + toolCall.function.name;
        }

        try {
            // JSON String arguments를 Map으로 변환 (여기서는 단순화를 위해 GSON 활용 가능)
            com.google.gson.Gson gson = new com.google.gson.Gson();
            Map<String, Object> args = gson.fromJson(toolCall.function.arguments, Map.class);
            
            Object result = tool.execute(args);
            return result != null ? result.toString() : "Success (No return value)";
            
        } catch (Exception e) {
            return "Error executing tool: " + e.getMessage();
        }
    }
}
