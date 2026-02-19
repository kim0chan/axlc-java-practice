package org.example.axlc.step4.tool;

import java.util.Map;

/**
 * AI(OpenAI GPT)가 사용할 수 있는 도구의 규격
 */
public interface Tool {
    /**
     * 도구의 고유 이름
     */
    String getName();

    /**
     * 도구에 대한 설명 (LLM이 이 설명을 보고 언제 사용할지 판단함)
     */
    String getDescription();

    /**
     * LLM에게 전달할 도구의 파라미터 명세 (JSON Schema 형태)
     */
    Map<String, Object> getParametersSchema();

    /**
     * 실제 도구 실행 로직
     * @param arguments LLM이 만들어준 인자값 (JSON 파싱 결과)
     * @return 실행 결과 (문자열 또는 객체)
     */
    Object execute(Map<String, Object> arguments);
}
