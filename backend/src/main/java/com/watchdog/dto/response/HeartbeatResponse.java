// dto/response/HeartbeatResponse.java
package com.watchdog.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HeartbeatResponse {
    private String deviceId;
    private String message;
    private long timeRemaining;
    private boolean resumed;
    private LocalDateTime timestamp;
}