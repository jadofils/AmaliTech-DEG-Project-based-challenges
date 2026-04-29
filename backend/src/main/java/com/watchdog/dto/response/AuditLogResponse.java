package com.watchdog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private Long id;
    private String monitorId;
    private String action;
    private LocalDateTime timestamp;
    private String details;
}
