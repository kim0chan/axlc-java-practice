package org.example.axlc.common.llm;

import io.github.cdimascio.dotenv.Dotenv;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AWS Bedrock API 구현체
 * AWS SDK for Java 2.x를 사용하여 Bedrock의 Converse API를 호출합니다.
 */
public class BedrockLlmClient implements LlmClient {
    private final BedrockRuntimeClient bedrockClient;
    private final String modelId;

    public BedrockLlmClient() {
        Dotenv dotenv = Dotenv.load();
        String accessKey = dotenv.get("AWS_ACCESS_KEY_ID");
        String secretKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
        String region = dotenv.get("AWS_REGION", "ap-southeast-2");
        this.modelId = dotenv.get("BEDROCK_MODEL_ID", "anthropic.claude-3-haiku-20240307-v1:0");

        if (accessKey == null || secretKey == null) {
            throw new IllegalArgumentException("AWS Credentials not found in .env file");
        }

        // AWS 자격 증명 및 클라이언트 설정
        this.bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();

        System.out.println("BedrockLlmClient initialized (Region: " + region + ", Model: " + modelId + ")");
    }

    @Override
    public String ask(List<ChatMessage> messages) {
        try {
            // ChatMessage 리스트를 Bedrock Message 리스트로 변환
            List<Message> bedrockMessages = messages.stream()
                    .filter(msg -> !msg.role.equalsIgnoreCase("system")) // System 메시지는 별도로 처리해야 함
                    .map(msg -> Message.builder()
                            .role(mapRole(msg.role))
                            .content(ContentBlock.fromText(msg.content))
                            .build())
                    .collect(Collectors.toList());

            // System 메시지 추출 (Converse API는 System 메시지를 별도 파라미터로 받음)
            String systemText = messages.stream()
                    .filter(msg -> msg.role.equalsIgnoreCase("system"))
                    .map(msg -> msg.content)
                    .findFirst()
                    .orElse("");

            // Converse API 호출
            ConverseResponse response = bedrockClient.converse(builder -> {
                builder.modelId(modelId)
                        .messages(bedrockMessages);
                
                if (!systemText.isEmpty()) {
                    builder.system(s -> s.text(systemText));
                }
            });

            // 응답 텍스트 추출
            return response.output().message().content().get(0).text();

        } catch (Exception e) {
            System.err.println("Bedrock API Error: " + e.getMessage());
            return "Error from Bedrock: " + e.getMessage();
        }
    }

    // Role 매핑 (user -> user, assistant -> assistant 등)
    private ConversationRole mapRole(String role) {
        if ("assistant".equalsIgnoreCase(role)) {
            return ConversationRole.ASSISTANT;
        }
        return ConversationRole.USER;
    }
}