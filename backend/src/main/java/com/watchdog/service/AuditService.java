package com.watchdog.service;

import com.watchdog.dto.response.AuditDeleteResponse;
import com.watchdog.dto.response.AuditLogResponse;
import com.watchdog.model.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface AuditService {
    void log(String monitorId, AuditAction action, Map<String, Object> details);
    List<AuditLogResponse> getLogsForDevice(String monitorId);
    Page<AuditLogResponse> getLogsForDevicePaged(String monitorId, Pageable pageable);
    List<AuditLogResponse> getLogsByAction(String action);
    Page<AuditLogResponse> getAllLogs(Pageable pageable);
    AuditDeleteResponse deleteLogsForDevice(String monitorId);
    AuditDeleteResponse deleteLogsByAction(String action);
    AuditDeleteResponse deleteOldLogs(int days);
}
