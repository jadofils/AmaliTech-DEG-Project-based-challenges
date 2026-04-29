package com.watchdog.controller;

import com.watchdog.dto.response.AuditDeleteResponse;
import com.watchdog.dto.response.AuditLogResponse;
import com.watchdog.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Query and manage the system audit trail. Every action performed on a monitor — CREATE, HEARTBEAT, PAUSE, RESUME, DELETE, and ALERT — is recorded with a timestamp and metadata. Use these endpoints to investigate device history, debug issues, or review alert patterns. Logs older than 30 days are automatically deleted daily at 3 AM.")
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping("/monitors/{id}")
    @Operation(
        summary = "Get audit logs for a device",
        description = "Returns the full history of all actions performed on a specific monitor, ordered from most recent to oldest."
    )
    public ResponseEntity<List<AuditLogResponse>> getLogsForDevice(
            @Parameter(description = "Device ID", required = true)
            @PathVariable String id) {
        return ResponseEntity.ok(auditService.getLogsForDevice(id));
    }

    @GetMapping("/monitors/{id}/paged")
    @Operation(
        summary = "Get paginated audit logs for a device",
        description = "Returns a paginated list of audit logs for a specific device."
    )
    public ResponseEntity<Page<AuditLogResponse>> getLogsForDevicePaged(
            @Parameter(description = "Device ID", required = true)
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getLogsForDevicePaged(
            id, PageRequest.of(page, size, Sort.by("timestamp").descending())
        ));
    }

    @GetMapping("/action/{action}")
    @Operation(
        summary = "Get audit logs by action type",
        description = "Returns all audit log entries for a specific action type across all devices. Valid actions: CREATE, HEARTBEAT, ALERT, PAUSE, RESUME, DELETE."
    )
    public ResponseEntity<List<AuditLogResponse>> getLogsByAction(
            @Parameter(description = "Action type: CREATE, HEARTBEAT, ALERT, PAUSE, RESUME, DELETE", required = true)
            @PathVariable String action) {
        return ResponseEntity.ok(auditService.getLogsByAction(action));
    }

    @GetMapping
    @Operation(
        summary = "Get all audit logs",
        description = "Returns a paginated list of all audit log entries across all devices, ordered by most recent first."
    )
    public ResponseEntity<Page<AuditLogResponse>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getAllLogs(
            PageRequest.of(page, size, Sort.by("timestamp").descending())
        ));
    }

    @DeleteMapping("/monitors/{id}")
    @Operation(
        summary = "Delete audit logs for a device",
        description = "Permanently deletes all audit log entries for a specific device."
    )
    public ResponseEntity<AuditDeleteResponse> deleteLogsForDevice(
            @Parameter(description = "Device ID", required = true)
            @PathVariable String id) {
        return ResponseEntity.ok(auditService.deleteLogsForDevice(id));
    }

    @DeleteMapping("/action/{action}")
    @Operation(
        summary = "Delete audit logs by action type",
        description = "Permanently deletes all audit log entries of a specific action type across all devices."
    )
    public ResponseEntity<AuditDeleteResponse> deleteLogsByAction(
            @Parameter(description = "Action type: CREATE, HEARTBEAT, ALERT, PAUSE, RESUME, DELETE", required = true)
            @PathVariable String action) {
        return ResponseEntity.ok(auditService.deleteLogsByAction(action));
    }

    @DeleteMapping("/older-than/{days}")
    @Operation(
        summary = "Delete audit logs older than N days",
        description = "Permanently deletes all audit log entries older than the specified number of days."
    )
    public ResponseEntity<AuditDeleteResponse> deleteOldLogs(
            @Parameter(description = "Number of days", required = true)
            @PathVariable int days) {
        return ResponseEntity.ok(auditService.deleteOldLogs(days));
    }
}
