// dto/response/MonitorResponse.java
package com.watchdog.dto.response;

import lombok.Builder;
import lombok.Data;
import com.watchdog.model.enums.MonitorStatus;
import java.time.LocalDateTime;

@Data
@Builder
public class MonitorResponse {
    private String id;
    private Integer timeout;
    private MonitorStatus status;
    private String alertEmail;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime createdAt;
    private String message;
}