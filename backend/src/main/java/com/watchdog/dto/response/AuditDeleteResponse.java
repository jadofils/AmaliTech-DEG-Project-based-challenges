package com.watchdog.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditDeleteResponse {
    private int deleted;
    private String message;
}
