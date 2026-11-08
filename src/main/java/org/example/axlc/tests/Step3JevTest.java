package org.example.axlc.tests;

import org.example.axlc.common.ConsoleColor;
import org.example.axlc.common.llm.jev.JevClient;
import org.example.axlc.common.llm.jev.JevModels.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jev (System One) Java 클라이언트 기능 테스트 (Latency 측정 포함)
 */
public class Step3JevTest {

    public static void main(String[] args) {
        System.out.println(ConsoleColor.CYAN + "=== Jev (System One) API 기능 테스트 ===" + ConsoleColor.RESET);

        try {
            JevClient client = new JevClient();

            // 1. Choice Test: 의도 분류
            System.out.println("\n[1] Choice API Test: 의도 분류");
            String stateChoice = "내일 오전 10시에 회의실 A 예약해줄래?";
            Map<String, String> criteria = new HashMap<>();
            criteria.put("meeting", "회의 예약 및 일정 관리");
            criteria.put("email", "이메일 작성 및 확인");
            criteria.put("chat", "단순 일상 대화");

            long start = System.currentTimeMillis();
            String resultChoice = client.choice(stateChoice, "이 요청의 카테고리를 선택하세요.", criteria);
            long end = System.currentTimeMillis();
            System.out.println("Input: " + stateChoice);
            System.out.println(ConsoleColor.GREEN + "Result: " + resultChoice + " (" + (end - start) + "ms)" + ConsoleColor.RESET);

            // 2. Noul Test: 이진 판단 (확률)
            System.out.println("\n[2] Noul API Test: 긴급도 판단");
            String stateNoul = "서버가 지금 죽었어! 당장 확인해줘!";
            start = System.currentTimeMillis();
            Double resultNoul = client.noul(stateNoul, "이 메시지는 긴급한 조치가 필요한가요?");
            end = System.currentTimeMillis();
            System.out.println("Input: " + stateNoul);
            System.out.println(ConsoleColor.GREEN + String.format("Probability (Yes): %.2f (%dms)", resultNoul, (end - start)) + ConsoleColor.RESET);

            // 3. Score Test: 감정 분석 (점수)
            System.out.println("\n[3] Score API Test: 감정 분석 (점수)");
            String stateScore = "배송이 너무 늦어서 정말 실망했어요. 다신 안 쓸 것 같네요.";
            List<String> rubric = List.of("매우 만족", "보통", "매우 불만족");

            start = System.currentTimeMillis();
            Double resultScore = client.score(stateScore, "고객의 불만 정도를 측정하세요.", rubric);
            end = System.currentTimeMillis();
            System.out.println("Input: " + stateScore);
            System.out.println(ConsoleColor.GREEN + String.format("Score (0=만족, 2=불만족): %.2f (%dms)", resultScore, (end - start)) + ConsoleColor.RESET);

            // 4. Multi-Decision Test: 한 번에 여러 개 묻기
            System.out.println("\n[4] Multi-Decision Test: 병렬 판단");
            String stateMulti = "대표님, 이번 달 매출 보고서가 준비되었습니다. 검토 부탁드립니다.";
            Map<String, JevQuestion> questions = new HashMap<>();
            questions.put("intent", new JevQuestion("choice", "의도 분류", Map.of("report", "보고서 제출", "ask", "질문")));
            questions.put("is_formal", new JevQuestion("noul", "격식 있는 말투인가요?"));
            questions.put("importance", new JevQuestion("score", "중요도 평가", List.of("낮음", "중간", "높음")));

            start = System.currentTimeMillis();
            JevResponse multiRes = client.decide(stateMulti, questions);
            end = System.currentTimeMillis();
            System.out.println("Input: " + stateMulti);
            System.out.println(ConsoleColor.GREEN + "Answers count: " + multiRes.answers.size() + " (" + (end - start) + "ms)" + ConsoleColor.RESET);
            multiRes.answers.forEach((k, v) -> {
                System.out.println(" - " + k + ": " + v.type + " (Choice: " + v.choice + ", Noul: " + v.noul + ", Score: " + v.score + ")");
            });

        } catch (Exception e) {
            System.err.println(ConsoleColor.RED + "테스트 중 오류 발생: " + e.getMessage() + ConsoleColor.RESET);
            e.printStackTrace();
        }
    }
}
