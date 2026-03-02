package org.example.axlc.step6;

import org.example.axlc.common.ConsoleColor;
import org.example.axlc.common.llm.ChatMessage;
import org.example.axlc.common.llm.LlmClient;
import org.example.axlc.common.llm.OpenAiLlmClient;

import java.util.List;

/**
 * [주제 2: LLM-as-a-Judge를 활용한 간단한 LLM Evaluation 구현]
 * LLM이 생성한 답변이 얼마나 정확한지(Faithfulness), 질문에 적절한지(Relevance) 다른 LLM Agent가 평가합니다.
 * RAGAS 같은 전문 라이브러리 없이도 '평가 파이프라인'을 이해할 수 있는 간단한 패턴입니다.
 */
public class Step6SimpleEval {

    private final LlmClient judgeClient;

    public Step6SimpleEval(LlmClient judgeClient) {
        this.judgeClient = judgeClient;
    }

    /**
     * 답변의 충실도(Faithfulness)를 1~5점으로 평가합니다.
     * @param context 검색된 맥락(검색 결과 등)
     * @param answer LLM이 생성한 답변
     * @return 1~5점 (정수형 점수만 반환하도록 유도)
     */
    public int evaluateFaithfulness(String context, String answer) {
        System.out.println(ConsoleColor.CYAN + "\n[Eval] 답변의 충실도를 평가 중입니다... " + ConsoleColor.RESET);

        String evalPrompt = """
            당신은 LLM이 생성한 답변의 '충실도(Faithfulness)'를 채점하는 엄격한 교수님입니다.
            주어진 '맥락(Context)'에 비추어 볼 때, '답변(Answer)'이 얼마나 사실에 근거했는지 판단하여 1~5점의 점수를 내려주세요.
            
            [채점 기준]
            - 5점: 답변의 모든 내용이 맥락에 정확히 명시되어 있음.
            - 3점: 답변의 일부 내용이 맥락에 없거나 추측이 섞여 있음.
            - 1점: 답변이 맥락과 상관없거나 거짓 정보를 포함하고 있음(환각 현상).
            
            맥락: "%s"
            답변: "%s"
            
            결과는 '점수: [정수]' 형식으로만 한 줄로 대답하세요 (예: 점수: 5).
            결과:""".formatted(context, answer);

        // 기존 ask 메서드를 사용하여 바로 String 응답을 받기
        String result = judgeClient.ask(List.of(
            new ChatMessage("user", evalPrompt)
        ));
        
        try {
            // 결과에서 숫자만 쏙 뽑아내기
            String scoreStr = result.replaceAll("[^0-9]", "");
            if (scoreStr.isEmpty()) return 1;
            return Integer.parseInt(scoreStr);
        } catch (Exception e) {
            System.out.println(ConsoleColor.CYAN + "[Eval] 점수 파싱 에러! 결과: " + result + ". 기본값 1점을 반환합니다." + ConsoleColor.RESET);
            return 1;
        }
    }

    public static void main(String[] args) {
        LlmClient client = new OpenAiLlmClient();
        Step6SimpleEval eval = new Step6SimpleEval(client);

        // 테스트 데이터 1: 완벽한 답변 (맥락에 근거함)
        String context1 = "회의실 A는 오후 2시부터 4시까지 예약이 불가능합니다.";
        String answer1 = "죄송합니다. 회의실 A는 오후 3시에 예약할 수 없습니다.";
        int score1 = eval.evaluateFaithfulness(context1, answer1);
        System.out.println("-> [평가 결과] 충실도 점수: " + score1 + "/5");

        // 테스트 데이터 2: 환각(Hallucination) 섞인 답변
        String context2 = "회의실 B는 오전에만 사용 가능합니다.";
        String answer2 = "회의실 B는 오후 5시에 예약 가능합니다. 커피도 무료로 제공됩니다."; // 맥락에 없는 내용
        int score2 = eval.evaluateFaithfulness(context2, answer2);
        System.out.println("-> [평가 결과] 충실도 점수: " + score2 + "/5");

        // 테스트 데이터 3: 애매한 답변
        String context3 = "회의실 B에서 오후 6시에 짜파게티 파티를 열기 위해 예약을 했습니다. 짜파게티 맛있게 끓이는 방법을 알려주세요.";
        String answer3 = "청양고추와 올리브유로 고추기름을 내어 함께 볶아보세요."; // ???
        int score3 = eval.evaluateFaithfulness(context3, answer3);
        System.out.println("-> [평가 결과] 충실도 점수: " + score3 + "/5");
    }
}
