package org.example.axlc.step3;

import org.example.axlc.step4.dto.CreateMeetingRequest;
import org.example.axlc.step4.dto.DeleteMeetingRequest;
import org.example.axlc.step4.dto.UpdateMeetingRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 가상의 미팅 예약 시스템 (Business Logic Layer)
 */
public class MeetingService {

    // ID 기반 인메모리 DB
    private final Map<Long, String> meetingDatabase = new ConcurrentHashMap<>();
    
    // 고유 ID 생성을 위한 AtomicLong (순차적 증가 보장)
    private final java.util.concurrent.atomic.AtomicLong idGenerator = 
        new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());

    /**
     * 신규 예약 생성
     */
    public String createMeeting(CreateMeetingRequest req) {
        // 중복 예약 체크
        if (isDuplicate(req.date, req.time)) {
            return String.format("ERROR: %s %s 시각은 이미 예약되어 있습니다. 다른 시간을 선택해 주세요.", req.date, req.time);
        }

        long id = idGenerator.incrementAndGet();
        String details = String.format("[ID: %d] %s %s (참석자: %s)", id, req.date, req.time, req.attendees);
        meetingDatabase.put(id, details);
        return "SUCCESS: 예약이 확정되었습니다. (예약번호: " + id + ")";
    }

    /**
     * 예약 수정
     */
    public String updateMeeting(UpdateMeetingRequest req) {
        if (!meetingDatabase.containsKey(req.id)) {
            return "ERROR: 해당 예약 번호(" + req.id + ")를 찾을 수 없습니다.";
        }

        // 중복 예약 체크
        if (isDuplicateExcluding(req.date, req.time, req.id)) {
            return String.format("ERROR: %s %s 시각은 이미 다른 예약이 잡혀 있습니다.", req.date, req.time);
        }

        String details = String.format("[ID: %d] %s %s (참석자: %s) [수정됨]", req.id, req.date, req.time, req.attendees);
        meetingDatabase.put(req.id, details);
        return "SUCCESS: 예약이 성공적으로 수정되었습니다.";
    }

    private boolean isDuplicate(String date, String time) {
        String target = date + " " + time;
        return meetingDatabase.values().stream()
                .anyMatch(details -> details.contains(target));
    }

    private boolean isDuplicateExcluding(String date, String time, long id) {
        String target = date + " " + time;
        return meetingDatabase.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(id))
                .anyMatch(entry -> entry.getValue().contains(target));
    }

    /**
     * 예약 취소
     */
    public String deleteMeeting(DeleteMeetingRequest req) {
        if (meetingDatabase.remove(req.id) != null) {
            return "SUCCESS: 예약 번호 " + req.id + "번이 취소되었습니다.";
        }
        return "ERROR: 해당 예약 번호(" + req.id + ")가 존재하지 않습니다.";
    }

    /**
     * 예약 목록 조회
     */
    public String getMeetingList(Object empty) {
        List<String> meetings = findAllMeetings();
        if (meetings.isEmpty()) return "현재 예약된 미팅이 없습니다.";
        return String.join("\n", meetings);
    }

    public List<String> findAllMeetings() {
        return new ArrayList<>(meetingDatabase.values());
    }
}
