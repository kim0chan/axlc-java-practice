package org.example.axlc.step4.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.axlc.common.ConsoleColor;
import org.example.axlc.step4.tool.McpTool;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SSE-based Remote MCP Client
 * URL(https://...)을 통해 원격 MCP 서버와 통신한다.
 */
public class SseMcpClient implements AutoCloseable {
    private final String connectionUrl;
    private String postUrl; 
    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final Map<Long, CompletableFuture<JsonObject>> pendingRequests = new ConcurrentHashMap<>();
    
    private boolean isNextLineEndpoint = false;

    public SseMcpClient(String connectionUrl) throws Exception {
        this(connectionUrl, null); // 기본적으로 헤더 없이 시작
    }

    public SseMcpClient(String connectionUrl, Map<String, String> headers) throws Exception {
        this.connectionUrl = connectionUrl.endsWith("/") ? connectionUrl.substring(0, connectionUrl.length() - 1) : connectionUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        // SSE 스트림 연결
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(connectionUrl))
                .header("Accept", "text/event-stream")
                .GET();

        // 만약 API Key 같은 헤더가 필요하면 추가
        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        System.out.println("[SseMcpClient] Connecting to " + connectionUrl + "...");
        
        httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> response.body().forEach(this::handleSseLine))
                .exceptionally(ex -> {
                    System.err.println("[SSE Error] " + ex.getMessage());
                    return null;
                });
        
        // Handshake 대기 (최대 10초)
        int retry = 0;
        while (postUrl == null && retry < 100) {
            Thread.sleep(100);
            retry++;
        }
        
        if (postUrl == null) {
            throw new IOException("MCP SSE 연결 실패: 서버로부터 POST 엔드포인트를 받지 못했습니다. (URL: " + connectionUrl + ")");
        }
        
        initialize();
    }

    private void handleSseLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return;

        // DEBUG LOG
        System.out.println(ConsoleColor.BLACK_BRIGHT + "[SSE Raw] " + trimmed + ConsoleColor.RESET);

        if (trimmed.startsWith("event: endpoint")) {
            isNextLineEndpoint = true;
        } else if (trimmed.startsWith("data: ")) {
            String data = trimmed.substring(6).trim();
            
            // 큰따옴표가 붙어있는 경우 제거 (JSON 형식 등 대비)
            if (data.startsWith("\"") && data.endsWith("\"")) {
                data = data.substring(1, data.length() - 1);
            }

            if (isNextLineEndpoint) {
                // 서버가 알려준 POST 엔드포인트 URL 저장
                this.postUrl = data.startsWith("http") ? data : connectionUrl + data;
                System.out.println(ConsoleColor.GREEN + "[SseMcpClient] Endpoint established: " + postUrl + ConsoleColor.RESET);
                isNextLineEndpoint = false;
            } else {
                // 일반 JSON-RPC 응답 처리
                try {
                    JsonObject response = gson.fromJson(data, JsonObject.class);
                    if (response.has("id")) {
                        long id = response.get("id").getAsLong();
                        CompletableFuture<JsonObject> future = pendingRequests.remove(id);
                        if (future != null) future.complete(response);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void initialize() throws Exception {
        JsonObject params = new JsonObject();
        params.addProperty("protocolVersion", "2024-11-05");
        
        JsonObject capabilities = new JsonObject();
        capabilities.add("resources", new JsonObject());
        capabilities.add("tools", new JsonObject());
        params.add("capabilities", capabilities);

        JsonObject clientInfo = new JsonObject();
        clientInfo.addProperty("name", "AXLC-Java-Remote-Client");
        clientInfo.addProperty("version", "1.0.0");
        params.add("clientInfo", clientInfo);

        sendRequest("initialize", params).get(10, TimeUnit.SECONDS);
    }

    public List<McpTool> getMcpTools() throws Exception {
        JsonObject response = sendRequest("tools/list", new JsonObject()).get(10, TimeUnit.SECONDS);
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
                    
                    mcpTools.add(new McpTool(null, name, description, inputSchema) {
                        @Override
                        public Object execute(Map<String, Object> arguments) {
                            try {
                                return callTool(name, arguments);
                            } catch (Exception e) {
                                return "ERROR: " + e.getMessage();
                            }
                        }
                    });
                }
            }
        }
        return mcpTools;
    }

    public String callTool(String name, Map<String, Object> arguments) throws Exception {
        JsonObject params = new JsonObject();
        params.addProperty("name", name);
        params.add("arguments", gson.toJsonTree(arguments));

        JsonObject response = sendRequest("tools/call", params).get(20, TimeUnit.SECONDS);
        if (response.has("result")) {
            JsonObject result = response.getAsJsonObject("result");
            if (result.has("content")) {
                JsonArray content = result.getAsJsonArray("content");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < content.size(); i++) {
                    JsonObject item = content.get(i).getAsJsonObject();
                    if (item.has("text")) sb.append(item.get("text").getAsString());
                }
                return sb.toString();
            }
        } else if (response.has("error")) {
            return "ERROR: " + response.get("error").toString();
        }
        return "ERROR: Tool execution failed";
    }

    private CompletableFuture<JsonObject> sendRequest(String method, JsonObject params) throws Exception {
        long id = idCounter.getAndIncrement();
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("id", id);
        request.addProperty("method", method);
        request.add("params", params);

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(postUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .build();

        httpClient.sendAsync(postRequest, HttpResponse.BodyHandlers.discarding());
        return future;
    }

    @Override
    public void close() {
        // SSE 연결은 BodyHandlers.ofLines()가 끝날 때 자동으로 닫힘
    }
}
