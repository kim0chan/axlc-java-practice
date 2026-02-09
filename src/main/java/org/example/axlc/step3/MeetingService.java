package org.example.axlc.step3;

import org.example.axlc.common.ConsoleColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 가상의 미팅 예약 시스템 (Business Logic Layer)
 * 실제 환경에서는 DB(MySQL, JPA)와 연동되는 Service 클래스가 될 것입니다.
 */
public class MeetingService {

    // In-Memory Database 역할
    private final List<String> meetingDatabase = new ArrayList<>();

    /**
     * 미팅 예약을 생성합니다.
     * @param details 예약 상세 정보 (날짜, 시간, 참석자 등)
     * @return 예약 결과 메시지
     */
    public String createMeeting(String details) {
        // 실제로는 여기서 DB Insert, 중복 체크, 이메일 발송 등의 로직이 수행됩니다.
        try {
            System.out.println(ConsoleColor.CYAN + "[MeetingService] DB 연결 및 트랜잭션 시작..." + ConsoleColor.RESET);
            Thread.sleep(500);  // DB I/O 시뮬레이션
            
            meetingDatabase.add(details);
            
            System.out.println(ConsoleColor.CYAN + "[MeetingService] 예약 데이터 저장 완료." + ConsoleColor.RESET);
            return "SUCCESS: 예약이 확정되었습니다. (예약번호: " + System.currentTimeMillis() + ")";
        } catch (InterruptedException e) {
            return "ERROR: 시스템 오류가 발생했습니다.";
        }
    }

    /**
     * 현재 예약된 모든 미팅 목록을 조회합니다.
     */
    public List<String> findAllMeetings() {
        return Collections.unmodifiableList(meetingDatabase);
    }
}
