package org.example.axlc.step3;

import org.example.axlc.common.ConsoleColor;
import org.example.axlc.common.LoadingSpinner;
import org.example.axlc.common.llm.ChatMessage;
import org.example.axlc.common.llm.LlmClient;
import org.example.axlc.common.llm.OpenAiLlmClient;
import org.example.axlc.step4.dto.CreateMeetingRequest;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Step 3: Primitive AI Agent
 * LLM의 판단(Reasoning)을 기반으로 행동(Action)하는 원시적인 에이전트를 구현합니다.
 * "NEED_INFO"와 "EXECUTE"라는 텍스트 패턴을 파싱하여 제어 흐름을 결정합니다.
 */
public class Step3PrimitiveAgent {

    // 🌟 비즈니스 로직을 담당하는 Service Layer (실제 시스템 연동 담당)
    private static final MeetingService meetingService = new MeetingService();

    public static void main(String[] args) throws Exception {
        // 출력을 UTF-8로 설정
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        // LLM Client 초기화
        LlmClient llmClient = new OpenAiLlmClient();

        System.out.println(ConsoleColor.CYAN + "=== [Step 3] 원시 에이전트: 스마트 오피스 비서 ===" + ConsoleColor.RESET);
        System.out.println("목표: 미팅 예약에 필요한 정보(날짜, 시간, 참석자)를 모두 수집하여 예약을 실행합니다.");
        printMeetingList(); // 초기 예약 현황 출력

        // 대화 히스토리 관리
        List<ChatMessage> messages = new ArrayList<>();

        // 1. 동적 시스템 프롬프트 생성 (현재 시간 주입)
        String systemPrompt = buildSystemPrompt();
        messages.add(new ChatMessage("system", systemPrompt));

        while (true) {
            System.out.print(ConsoleColor.BLUE + "[User]: " + ConsoleColor.RESET);
            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("대화를 종료합니다. Bye!");
                break;
            }

            // 사용자 입력을 컨텍스트에 추가
            messages.add(new ChatMessage("user", input));

            // LLM에게 현재 상황 판단 요청
            String response = askLlmWithSpinner(llmClient, messages);

            // 🌟 2. 판단 결과 분석 (Text Parsing)
            if (response.startsWith("NEED_INFO:")) {
                // 🌟 Case A: 정보 부족 -> 사용자에게 추가 질문 (노란색으로 강조)
                String question = response.replace("NEED_INFO:", "").trim();
                System.out.println(ConsoleColor.YELLOW + "[Need Info]: " + ConsoleColor.RESET + question);
                
                // 대화 히스토리에 추가
                messages.add(new ChatMessage("assistant", response));
                
            } else if (response.startsWith("EXECUTE:")) {
                // 🌟 Case B: 정보 충족 -> 실제 액션(Action) 실행
                System.out.println(ConsoleColor.PURPLE + "[Agent Action]: " + ConsoleColor.RESET + "명령을 인식했습니다. 예약 시스템을 호출합니다...");
                System.out.println(">> " + response);
                
                // 실제 시스템(Service) 호출
                String actionResult = executeAction(response);
                
                // 실행 결과를 사용자에게 보여줌 (UI 업데이트)
                printMeetingList();
                System.out.println(ConsoleColor.CYAN + "[System]: " + actionResult + ConsoleColor.RESET);

                // 🌟 3. 실행 결과를 다시 컨텍스트에 추가 (LLM이 다음 턴에 이를 기억하도록 함)
                messages.add(new ChatMessage("assistant", response));
                messages.add(new ChatMessage("user", "시스템 실행 결과: " + actionResult));
                
            } else {
                // Case C: 일반 대화
                System.out.println(ConsoleColor.GREEN + "[AI]: " + ConsoleColor.RESET + response);
                messages.add(new ChatMessage("assistant", response));
            }
        }
    }

    /**
     * 현재 날짜와 요일을 포함한 시스템 프롬프트를 생성합니다.
     */
    private static String buildSystemPrompt() {
        LocalDate today = LocalDate.now();
        String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
        String todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 🌟 TIP: 실제로는 "무언가를 하지 말라"는 식의 "Don't Prompting"은 좋지 않다고 여러 BP 사례에서 언급하고 있습니다.
        return """
                # Basic Information
                - 당신은 스마트 오피스의 미팅 예약 에이전트입니다.
                - 현재 시각은 %s %s입니다. (이 정보를 바탕으로 날짜를 추론하세요.)
                
                # Rules
                - 이용자가 미팅 예약과 관련 없는 대화를 진행할 경우, **미팅 예약과 관련된 프롬프트를 언급하지 말고** 평범하게 대화를 이어가세요.
                - 실제 이용자에게 제공할 수 있는 능력 안에서만 대화하세요.
                - 만약 미팅 예약을 위한 정보가 하나라도 부족하면 반드시 "NEED_INFO: {부족한 정보에 대한 질문}" 형식으로 답변하세요.
                - 미팅 예약을 위한 모든 정보가 수집되었다면 반드시 "EXECUTE: CREATE_MEETING(date='...', time='...', attendees='...')" 형식으로 답변하세요.
                
                # Scheduling A Meeting
                사용자가 미팅 예약을 요청하면 다음 3가지 정보가 반드시 필요합니다:
                1. 날짜 (예: 2026-02-11)
                2. 시간 (예: 14:00)
                3. 참석자 (예: 김철수, 이영희)
                
                ___
                대화의 흐름에 따라 유연하게 대처하세요. 굿럭!
                """.formatted(todayStr, dayOfWeek);
    }

    private static String executeAction(String command) {
        // EXECUTE: CREATE_MEETING(date='...', time='...', attendees='...') 에서 값 추출
        CreateMeetingRequest req = new CreateMeetingRequest();
        
        req.date = extractValue(command, "date");
        req.time = extractValue(command, "time");
        req.attendees = extractValue(command, "attendees");

        return meetingService.createMeeting(req);
    }

    private static String extractValue(String command, String key) {
        try {
            int start = command.indexOf(key + "='") + key.length() + 2;
            int end = command.indexOf("'", start);
            return command.substring(start, end);
        } catch (Exception e) {
            return "";
        }
    }

    private static void printMeetingList() {
        List<String> meetings = meetingService.findAllMeetings();
        System.out.println(ConsoleColor.CYAN + "--------------------------------------------------");
        System.out.println("📅 현재 예약된 미팅 목록 (Total: " + meetings.size() + ")");
        if (meetings.isEmpty()) {
            System.out.println("   (예약 없음)");
        } else {
            for (int i = 0; i < meetings.size(); i++) {
                System.out.println("   " + (i + 1) + ". " + meetings.get(i));
            }
        }
        System.out.println("--------------------------------------------------" + ConsoleColor.RESET);
    }

    private static String askLlmWithSpinner(LlmClient client, List<ChatMessage> messages) throws Exception {
        LoadingSpinner spinner = new LoadingSpinner();
        Thread thread = new Thread(spinner);
        thread.start();

        String response = client.ask(messages);

        spinner.stop();
        thread.join();
        return response;
    }
}
