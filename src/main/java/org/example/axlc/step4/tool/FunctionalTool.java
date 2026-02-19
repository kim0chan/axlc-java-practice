package org.example.axlc.step4.tool;

import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 함수(Method)를 기반으로 자동 스키마 생성을 지원하는 도구 구현체
 */
public class FunctionalTool implements Tool {
    private final Object target;
    private final Method method;
    private final String name;
    private final String description;
    private final Map<String, Object> schema;
    private final Class<?> argsClass;

    public FunctionalTool(Object target, String methodName, String name, String description) {
        this.target = target;
        this.name = name;
        this.description = description;

        // reflection으로 메소드 찾기 (첫 번째 파라미터를 args 객체로 간주)
        try {
            this.method = findMethod(target.getClass(), methodName);
            this.argsClass = method.getParameterTypes()[0];
            this.schema = generateSchema(this.argsClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize FunctionalTool: " + e.getMessage());
        }
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getDescription() { return description; }

    @Override
    public Map<String, Object> getParametersSchema() { return schema; }

    @Override
    public Object execute(Map<String, Object> arguments) {
        try {
            // JSON Map을 인자 객체(argsClass)로 변환
            Gson gson = new Gson();
            String json = gson.toJson(arguments);
            Object argsObj = gson.fromJson(json, argsClass);

            // 메소드 실행
            return method.invoke(target, argsObj);
        } catch (Exception e) {
            return "Error executing tool: " + e.getMessage();
        }
    }

    private Method findMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == 1) {
                return m;
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    /**
     * 클래스 정보를 바탕으로 JSON Schema 자동 생성
     */
    private Map<String, Object> generateSchema(Class<?> clazz) {
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            Map<String, Object> fieldSpec = new HashMap<>();
            
            // 타입 매핑
            String type = "string";
            if (field.getType() == int.class || field.getType() == Integer.class ||
                field.getType() == double.class || field.getType() == Double.class) {
                type = "number";
            } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                type = "boolean";
            }
            fieldSpec.put("type", type);

            // 어노테이션에서 설명 읽기
            ToolParam ann = field.getAnnotation(ToolParam.class);
            if (ann != null) {
                fieldSpec.put("description", ann.description());
                if (ann.required()) required.add(field.getName());
            }

            properties.put(field.getName(), fieldSpec);
        }

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) schema.put("required", required);
        
        return schema;
    }
}
