package org.example.axlc.step4.dto;

import org.example.axlc.step4.tool.ToolParam;

public class UpdateMeetingRequest {
    @ToolParam(description = "수정할 예약의 고유 ID (예약 번호)")
    public long id;

    @ToolParam(description = "변경할 날짜 (YYYY-MM-DD)")
    public String date;

    @ToolParam(description = "변경할 시간 (HH:mm)")
    public String time;

    @ToolParam(description = "변경할 참석자 명단")
    public String attendees;
}
