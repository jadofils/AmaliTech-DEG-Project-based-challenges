package com.watchdog.dto.response;

import com.watchdog.model.enums.MonitorStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StatusResponse {
    private String id;
    private MonitorStatus status;
    private int timeout;
    private long timeRemaining;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime createdAt;
    private int alertCount;
    private String alertEmail;
}
