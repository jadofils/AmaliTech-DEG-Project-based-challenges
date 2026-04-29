package com.watchdog.controller;

import com.watchdog.model.entity.AuditLog;
import com.watchdog.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Query and manage the system audit trail. Every action performed on a monitor — CREATE, HEARTBEAT, PAUSE, RESUME, DELETE, and ALERT — is recorded with a timestamp and metadata. Use these endpoints to investigate device history, debug issues, or review alert patterns. Logs older than 30 days are automatically deleted daily at 3 AM.")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/monitors/{id}")
    @Operation(
        summary = "Get audit logs for a device",
        description = "Returns the full history of all actions performed on a specific monitor, ordered from most recent to oldest. Useful for investigating why a device went DOWN or how many times it has been paused."
    )
    public ResponseEntity<List<AuditLog>> getLogsForDevice(
            @Parameter(description = "Device ID", required = true)
            @PathVariable String id) {
        return ResponseEntity.ok(auditLogRepository.findByMonitorIdOrderByTimestampDesc(id));
    }

    @GetMapping("/monitors/{id}/paged")
    @Operation(
        summary = "Get paginated audit logs for a device",
        description = "Returns a paginated list of audit logs for a specific device. Use this for devices with long histories to avoid large responses."
    )
    public ResponseEntity<Page<AuditLog>> getLogsForDevicePaged(
            @Parameter(description = "Device ID", required = true)
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            auditLogRepository.findByMonitorId(id, PageRequest.of(page, size, Sort.by("timestamp").descending()))
        );
    }

    @GetMapping("/action/{action}")
    @Operation(
        summary = "Get audit logs by action type",
        description = "Returns all audit log entries for a specific action type across all devices. Valid actions: CREATE, HEARTBEAT, ALERT, PAUSE, RESUME, DELETE. For example, querying ALERT shows every device that has ever gone DOWN."
    )
    public ResponseEntity<List<AuditLog>> getLogsByAction(
            @Parameter(description = "Action type: CREATE, HEARTBEAT, ALERT, PAUSE, RESUME, DELETE", required = true)
            @PathVariable String action) {
        return ResponseEntity.ok(auditLogRepository.findByAction(action.toUpperCase()));
    }

    @GetMapping
    @Operation(
        summary = "Get all audit logs",
        description = "Returns a paginated list of all audit log entries across all devices, ordered by most recent first. Use this for a system-wide activity feed or compliance reporting."
    )
    public ResponseEntity<Page<AuditLog>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            auditLogRepository.findAll(PageRequest.of(page, size, Sort.by("timestamp").descending()))
        );
    }

    @DeleteMapping("/monitors/{id}")
    @Transactional
    @Operation(
        summary = "Delete audit logs for a device",
        description = "Permanently deletes all audit log entries for a specific device. Use this when decommissioning a device and you want to clean up its history."
    )
    public ResponseEntity<Map<String, Object>> deleteLogsForDevice(
            @Parameter(description = "Device ID", required = true)
            @PathVariable String id) {
        int deleted = auditLogRepository.deleteByMonitorId(id);
        return ResponseEntity.ok(Map.of("deleted", deleted, "monitorId", id));
    }

    @DeleteMapping("/action/{action}")
    @Transactional
    @Operation(
        summary = "Delete audit logs by action type",
        description = "Permanently deletes all audit log entries of a specific action type across all devices. Valid actions: CREATE, HEARTBEAT, ALERT, PAUSE, RESUME, DELETE."
    )
    public ResponseEntity<Map<String, Object>> deleteLogsByAction(
            @Parameter(description = "Action type: CREATE, HEARTBEAT, ALERT, PAUSE, RESUME, DELETE", required = true)
            @PathVariable String action) {
        int deleted = auditLogRepository.deleteByAction(action.toUpperCase());
        return ResponseEntity.ok(Map.of("deleted", deleted, "action", action.toUpperCase()));
    }

    @DeleteMapping("/older-than/{days}")
    @Transactional
    @Operation(
        summary = "Delete audit logs older than N days",
        description = "Permanently deletes all audit log entries older than the specified number of days. The scheduled cleanup runs this automatically every day at 3 AM for logs older than 30 days."
    )
    public ResponseEntity<Map<String, Object>> deleteOldLogs(
            @Parameter(description = "Number of days. Logs older than this will be deleted.", required = true)
            @PathVariable int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        int deleted = auditLogRepository.deleteByTimestampBefore(cutoff);
        return ResponseEntity.ok(Map.of("deleted", deleted, "olderThanDays", days));
    }
}
