package org.example.axlc.common;

/**
 * 콘솔 로딩 애니메이션을 담당하는 스피너
 */
public class LoadingSpinner implements Runnable {
    private volatile boolean running = true;
    private final String[] frames = {"|", "/", "-", "\\"};
    private final long interval = 50;

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        int i = 0;
        while (running) {
            System.out.print("\r" + ConsoleColor.YELLOW + "LLM은 생각 중... " + frames[i++ % frames.length] + ConsoleColor.RESET);
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // 마지막에 해당 라인을 공백으로 덮어씌워서 깔끔하게 지움
        System.out.print("\r" + " ".repeat(40) + "\r");
    }
}
