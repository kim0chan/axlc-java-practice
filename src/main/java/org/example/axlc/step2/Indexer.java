package org.example.axlc.step2;

import org.example.axlc.common.llm.ChatMessage;
import org.example.axlc.common.llm.LlmClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Indexer {
    private final LlmClient llmClient;
    private static final String RAW_DIR = "src/main/resources/data/raw";
    private static final String KB_DIR = "src/main/resources/data/knowledge-base";

    public Indexer(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public void runIndexing() throws IOException {
        Path kbPath = Paths.get(KB_DIR);
        // KB 폴더가 있고 내용이 있으면 건너뛰기
        if (Files.exists(kbPath) && Files.list(kbPath).findAny().isPresent()) {
            System.out.println(">>> Knowledge Base가 이미 존재합니다. 인덱싱을 건너뜁니다.");
            return;
        }

        Files.createDirectories(kbPath);
        Path rawFilePath = Paths.get(RAW_DIR, "axlc_lecture.txt");
        if (!Files.exists(rawFilePath)) {
            System.out.println(">>> 원본 파일이 없습니다: " + rawFilePath);
            return;
        }

        String content = Files.readString(rawFilePath);
        System.out.println(">>> 인덱싱 시작 (LLM에게 청킹 요청 중...).");

        // LLM에게 청킹 요청
        String prompt = "다음은 AI 응용 개발 강의 문서입니다. 이를 나중에 검색하기 좋게 '의미 있는 소주제' 단위로 쪼개주세요.\n" +
                "각 조각에 대해 파일명(영어, .txt)과 내용을 다음 형식으로 출력해주세요.\n" +
                "형식: --- FILENAME: [파일명] --- [내용] --- END ---\\n\n" +
                "문서 내용:\n" + content;

        String response = llmClient.ask(List.of(new ChatMessage("user", prompt)));

        // 정규표현식으로 파일 분리 및 저장
        Pattern pattern = Pattern.compile("--- FILENAME: (.*?) ---(.*?)--- END ---", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);

        int count = 0;
        while (matcher.find()) {
            String fileName = matcher.group(1).trim();
            String fileContent = matcher.group(2).trim();
            Files.writeString(kbPath.resolve(fileName), fileContent);
            System.out.println(">>> 지식 조각 저장됨: " + fileName);
            count++;
        }
        
        if (count == 0) {
            System.out.println(">>> 경고: LLM 응답에서 파일을 추출하지 못했습니다. 응답 형식 확인 필요.");
            System.out.println(">>> 응답 내용: " + response);
        } else {
            System.out.println(">>> 인덱싱 완료! (" + count + "개 파일 생성)");
        }
    }
}
