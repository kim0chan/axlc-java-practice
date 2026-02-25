package org.example.axlc.step4.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.axlc.step4.tool.McpTool;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Basic MCP (Model Context Protocol) Client Implementation
 * Simple stdio-based JSON-RPC communication for practice.
 */
public class McpClient implements AutoCloseable {
    private final Process process;
    private final BufferedWriter writer;
    private final BufferedReader reader;
    private final Gson gson = new Gson();
    private final AtomicLong idCounter = new AtomicLong(1);

    public McpClient(String... command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT); // 서버 에러 로그(stderr)는 터미널에 바로 출력
        this.process = pb.start();
        
        this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        
        // Handshake
        initialize();
    }

    private void initialize() throws IOException {
        JsonObject params = new JsonObject();
        params.addProperty("protocolVersion", "2024-11-05");
        
        JsonObject capabilities = new JsonObject();
        capabilities.add("resources", new JsonObject());
        capabilities.add("tools", new JsonObject());
        params.add("capabilities", capabilities);

        JsonObject clientInfo = new JsonObject();
        clientInfo.addProperty("name", "AXLC-Java-Client");
        clientInfo.addProperty("version", "1.0.0");
        params.add("clientInfo", clientInfo);

        // Initialize 요청을 보내고 응답을 기다린다.
        JsonObject response = sendRequest("initialize", params);
        
        if (response.has("error")) {
            throw new IOException("MCP 서버 초기화 실패: " + response.get("error"));
        }

        // 초기화 완료 알림을 보냅니다.
        sendNotification("notifications/initialized", new JsonObject());
    }

    /**
     * MCP 서버로부터 사용 가능한 도구 목록을 McpTool 리스트로 가져온다.
     */
    public List<McpTool> getMcpTools() throws IOException {
        JsonObject response = sendRequest("tools/list", new JsonObject());
        List<McpTool> mcpTools = new ArrayList<>();
        if (response.has("result")) {
            JsonObject result = response.getAsJsonObject("result");
            if (result.has("tools")) {
                JsonArray toolArray = result.getAsJsonArray("tools");
                for (int i = 0; i < toolArray.size(); i++) {
                    JsonObject tool = toolArray.get(i).getAsJsonObject();
                    String name = tool.get("name").getAsString();
                    String description = tool.get("description").getAsString();
                    Map<String, Object> inputSchema = gson.fromJson(tool.get("inputSchema"), Map.class);
                    
                    // Normalize schema: Ensure it's not null and has a valid type
                    if (inputSchema == null || "None".equals(inputSchema.get("type"))) {
                        inputSchema = new java.util.HashMap<>();
                        inputSchema.put("type", "object");
                        inputSchema.put("properties", new java.util.HashMap<>());
                    }
                    
                    mcpTools.add(new McpTool(this, name, description, inputSchema));
                }
            }
        }
        return mcpTools;
    }

    /**
     * MCP 서버의 도구를 실행한다.
     */
    public String callTool(String name, Map<String, Object> arguments) throws IOException {
        JsonObject params = new JsonObject();
        params.addProperty("name", name);
        params.add("arguments", gson.toJsonTree(arguments));

        JsonObject response = sendRequest("tools/call", params);
        if (response.has("result")) {
            JsonObject result = response.getAsJsonObject("result");
            if (result.has("content")) {
                JsonArray content = result.getAsJsonArray("content");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < content.size(); i++) {
                    JsonObject item = content.get(i).getAsJsonObject();
                    if (item.has("text")) {
                        sb.append(item.get("text").getAsString());
                    }
                }
                return sb.toString();
            }
        } else if (response.has("error")) {
            return "ERROR: " + response.get("error").toString();
        }
        return "ERROR: Tool execution failed with unknown response";
    }

    private JsonObject sendRequest(String method, JsonObject params) throws IOException {
        long id = idCounter.getAndIncrement();
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("id", id);
        request.addProperty("method", method);
        request.add("params", params);

        String json = gson.toJson(request);
        writer.write(json);
        writer.newLine();
        writer.flush();

        // 응답을 읽어들인다.
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                if (!process.isAlive()) {
                    throw new IOException("MCP 서버 프로세스가 종료되었습니다.");
                }
                throw new IOException("MCP 서버로부터 빈 응답을 받았습니다.");
            }
            
            try {
                JsonObject response = gson.fromJson(line, JsonObject.class);
                // 우리가 보낸 ID와 일치하는 응답만 반환
                if (response.has("id") && response.get("id").getAsLong() == id) {
                    return response;
                }
                // ID가 없거나 다르면 Notification이거나 다른 요청에 대한 응답이므로 계속 읽음
            } catch (Exception e) {
                // JSON 파싱 실패 시 무시 (서버의 다른 로그 메시지 등)
            }
        }
    }

    private void sendNotification(String method, JsonObject params) throws IOException {
        JsonObject notification = new JsonObject();
        notification.addProperty("jsonrpc", "2.0");
        notification.addProperty("method", method);
        notification.add("params", params);

        writer.write(gson.toJson(notification));
        writer.newLine();
        writer.flush();
    }

    @Override
    public void close() {
        process.destroy();
    }
}
