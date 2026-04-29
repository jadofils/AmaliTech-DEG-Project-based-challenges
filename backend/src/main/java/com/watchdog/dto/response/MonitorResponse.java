package com.watchdog.dto.response;

import com.watchdog.model.enums.MonitorStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MonitorResponse {
    private String id;
    private int timeout;
    private MonitorStatus status;
    private String alertEmail;
    private String alertWebhook;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime createdAt;
    private int alertCount;
    private String message;
}
