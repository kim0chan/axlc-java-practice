package org.example.axlc.step4.dto;

import org.example.axlc.step4.tool.ToolParam;

public class CreateMeetingRequest {
    @ToolParam(description = "날짜 (YYYY-MM-DD)")
    public String date;

    @ToolParam(description = "시간 (HH:mm)")
    public String time;

    @ToolParam(description = "참석자 명단")
    public String attendees;
}