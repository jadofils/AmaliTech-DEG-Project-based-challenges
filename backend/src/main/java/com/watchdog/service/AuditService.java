package com.watchdog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.watchdog.model.entity.AuditLog;
import com.watchdog.model.enums.AuditAction;
import com.watchdog.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

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
}
