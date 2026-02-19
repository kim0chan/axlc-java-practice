package org.example.axlc.step4.dto;

import org.example.axlc.step4.tool.ToolParam;

public class DeleteMeetingRequest {
    @ToolParam(description = "취소할 예약의 고유 ID (예약 번호)")
    public long id;
}
