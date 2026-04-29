package com.watchdog.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.watchdog.dto.response.AuditDeleteResponse;
import com.watchdog.dto.response.AuditLogResponse;
import com.watchdog.model.entity.AuditLog;
import com.watchdog.model.enums.AuditAction;
import com.watchdog.repository.AuditLogRepository;
import com.watchdog.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    @Async
    public void log(String monitorId, AuditAction action, Map<String, Object> details) {
        try {
            String detailsJson = objectMapper.writeValueAsString(details);
            AuditLog entry = AuditLog.builder()
                    .monitorId(monitorId)
                    .action(action.name())
                    .details(detailsJson)
                    .build();
            auditLogRepository.save(entry);
            log.debug("Audit logged: {} - {}", action, monitorId);
        } catch (Exception e) {
            log.error("Failed to write audit log for device: {}", monitorId, e);
        }
    }

    @Override
    public List<AuditLogResponse> getLogsForDevice(String monitorId) {
        return auditLogRepository.findByMonitorIdOrderByTimestampDesc(monitorId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public Page<AuditLogResponse> getLogsForDevicePaged(String monitorId, Pageable pageable) {
        return auditLogRepository.findByMonitorId(monitorId, pageable).map(this::toResponse);
    }

    @Override
    public List<AuditLogResponse> getLogsByAction(String action) {
        return auditLogRepository.findByAction(action.toUpperCase())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public Page<AuditLogResponse> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public AuditDeleteResponse deleteLogsForDevice(String monitorId) {
        int deleted = auditLogRepository.deleteByMonitorId(monitorId);
        log.info("Deleted {} audit logs for device: {}", deleted, monitorId);
        return AuditDeleteResponse.builder()
                .deleted(deleted)
                .message("Deleted " + deleted + " audit logs for device: " + monitorId)
                .build();
    }

    @Override
    @Transactional
    public AuditDeleteResponse deleteLogsByAction(String action) {
        int deleted = auditLogRepository.deleteByAction(action.toUpperCase());
        log.info("Deleted {} audit logs for action: {}", deleted, action);
        return AuditDeleteResponse.builder()
                .deleted(deleted)
                .message("Deleted " + deleted + " audit logs for action: " + action.toUpperCase())
                .build();
    }

    @Override
    @Transactional
    public AuditDeleteResponse deleteOldLogs(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        int deleted = auditLogRepository.deleteByTimestampBefore(cutoff);
        log.info("Deleted {} audit logs older than {} days", deleted, days);
        return AuditDeleteResponse.builder()
                .deleted(deleted)
                .message("Deleted " + deleted + " audit logs older than " + days + " days")
                .build();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .monitorId(log.getMonitorId())
                .action(log.getAction())
                .timestamp(log.getTimestamp())
                .details(log.getDetails())
                .build();
    }
}
