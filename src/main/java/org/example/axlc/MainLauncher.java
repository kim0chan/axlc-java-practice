package org.example.axlc;

import org.example.axlc.step0.Step0StatelessChat;
import org.example.axlc.step1.Step1ContextChat;
import org.example.axlc.step2.Step2Main;
import org.example.axlc.step3.Step3PrimitiveAgent;
import org.example.axlc.step4.Step4ToolCallAgent;
import org.example.axlc.step4.Step4McpAgent;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class MainLauncher {
    public static void main(String[] args) throws Exception {
        // 시스템 전역 입출력 인코딩 강제 설정 (Windows 한글 깨짐 방지)
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        if (args.length == 0) {
            System.out.println("Usage: gradlew run --args=\"step0|step1|step2|step3|step4|step4-mcp\"");
            return;
        }

        String step = args[0].toLowerCase();
        switch (step) {
            case "step0":
                Step0StatelessChat.main(args);
                break;
            case "step1":
                Step1ContextChat.main(args);
                break;
            case "step2":
                Step2Main.main(args);
                break;
            case "step3":
                Step3PrimitiveAgent.main(args);
                break;
            case "step4":
                Step4ToolCallAgent.main(args);
                break;
            case "step4-mcp":
                Step4McpAgent.main(args);
                break;
            default:
                System.out.println("Unknown step: " + step);
                System.out.println("Available steps: step0, step1, step2, step3, step4, step4-mcp");
        }
    }
}
