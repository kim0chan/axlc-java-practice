package org.example.axlc;

import org.example.axlc.step0.Step0StatelessChat;
import org.example.axlc.step1.Step1ContextChat;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class MainLauncher {
    public static void main(String[] args) throws Exception {
        // 시스템 전역 입출력 인코딩 강제 설정 (Windows 한글 깨짐 방지)
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        if (args.length == 0) {
            System.out.println("Usage: gradlew run --args=\"step0|step1\"");
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
            default:
                System.out.println("Unknown step: " + step);
                System.out.println("Available steps: step0, step1");
        }
    }
}
