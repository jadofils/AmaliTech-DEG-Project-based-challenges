package com.watchdog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HeartbeatResponse {
    private String deviceId;
    private String message;
    private long timeRemaining;
    private boolean resumed;
    private LocalDateTime timestamp;
}
