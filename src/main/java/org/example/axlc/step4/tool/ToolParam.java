package org.example.axlc.step4.tool;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 도구의 파라미터 필드에 설명을 달기 위한 어노테이션
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolParam {
    String description();
    boolean required() default true;
}
