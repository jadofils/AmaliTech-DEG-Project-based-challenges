// service/impl/MonitorServiceImpl.java
package com.watchdog.service.impl;

import com.watchdog.dto.request.CreateMonitorRequest;
import com.watchdog.dto.response.HeartbeatResponse;
import com.watchdog.dto.response.MonitorResponse;
import com.watchdog.dto.response.StatusResponse;
import com.watchdog.exception.MonitorAlreadyExistsException;
import com.watchdog.exception.MonitorExpiredException;
import com.watchdog.exception.MonitorNotFoundException;
import com.watchdog.model.entity.Monitor;
import com.watchdog.model.enums.AuditAction;
import com.watchdog.model.enums.MonitorStatus;
import com.watchdog.repository.MonitorRepository;
import com.watchdog.service.AlertService;
import com.watchdog.service.AuditService;
import com.watchdog.service.MonitorService;
import com.watchdog.service.TimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MonitorServiceImpl implements MonitorService {
    
    private final MonitorRepository monitorRepository;
    private final TimerService timerService;
    private final AlertService alertService;
    private final AuditService auditService;
    
    @Override
    public MonitorResponse createMonitor(CreateMonitorRequest request) {
        log.info("Creating monitor for device: {}", request.getDeviceId());
        
        // Check if monitor already exists
        if (monitorRepository.existsById(request.getDeviceId())) {
            throw new MonitorAlreadyExistsException(request.getDeviceId());
        }
        
        // Create new monitor entity
        Monitor monitor = Monitor.builder()
            .id(request.getDeviceId())
            .timeout(request.getTimeout())
            .alertEmail(request.getAlertEmail())
            .alertWebhook(request.getAlertWebhook())
            .status(MonitorStatus.ACTIVE)
            .lastHeartbeat(LocalDateTime.now())
            .alertCount(0)
            .build();
        
        Monitor savedMonitor = monitorRepository.save(monitor);
        
        // Start the timer
        timerService.startTimer(savedMonitor.getId(), savedMonitor.getTimeout());
        
        auditService.log(savedMonitor.getId(), AuditAction.CREATE, Map.of(
            "timeout", savedMonitor.getTimeout(),
            "alertEmail", savedMonitor.getAlertEmail()
        ));
        
        log.info("Monitor created successfully for device: {}", request.getDeviceId());
        
        return MonitorResponse.builder()
            .id(savedMonitor.getId())
            .timeout(savedMonitor.getTimeout())
            .status(savedMonitor.getStatus())
            .alertEmail(savedMonitor.getAlertEmail())
            .lastHeartbeat(savedMonitor.getLastHeartbeat())
            .createdAt(savedMonitor.getCreatedAt())
            .message("Monitor created successfully")
            .build();
    }
    
    @Override
    public HeartbeatResponse sendHeartbeat(String deviceId) {
        log.info("Received heartbeat for device: {}", deviceId);
        
        Monitor monitor = monitorRepository.findById(deviceId)
            .orElseThrow(() -> new MonitorNotFoundException("Monitor not found for device: " + deviceId));
        
        // Check if monitor is expired
        if (monitor.getStatus() == MonitorStatus.DOWN) {
            throw new MonitorExpiredException(deviceId);
        }
        
        // If paused, auto-resume
        boolean wasResumed = false;
        if (monitor.getStatus() == MonitorStatus.PAUSED) {
            log.info("Auto-resuming paused monitor for device: {}", deviceId);
            resumeMonitor(deviceId);
            monitor = monitorRepository.findById(deviceId).get();
            wasResumed = true;
        }
        
        // Update last heartbeat
        LocalDateTime heartbeatTime = LocalDateTime.now();
        monitorRepository.updateLastHeartbeat(deviceId, heartbeatTime);
        
        // Reset timer
        timerService.resetTimer(deviceId, monitor.getTimeout());
        
        // Calculate time remaining
        long timeRemaining = ChronoUnit.SECONDS.between(heartbeatTime,
            heartbeatTime.plusSeconds(monitor.getTimeout()));

        auditService.log(deviceId, AuditAction.HEARTBEAT, Map.of(
            "timeRemaining", timeRemaining
        ));

        log.info("Heartbeat processed for device: {}. Timer reset to {} seconds", deviceId, monitor.getTimeout());
        
        return HeartbeatResponse.builder()
            .deviceId(deviceId)
            .message(wasResumed ? "Heartbeat received - monitor resumed" : "Heartbeat received - timer reset")
            .timeRemaining((long) timeRemaining)
            .resumed(wasResumed)
            .timestamp(heartbeatTime)
            .build();
    }
    
    @Override
    public StatusResponse getMonitorStatus(String deviceId) {
        Monitor monitor = monitorRepository.findById(deviceId)
            .orElseThrow(() -> new MonitorNotFoundException("Monitor not found for device: " + deviceId));
        
        long timeRemaining = timerService.getTimeRemaining(deviceId);
        
        return StatusResponse.builder()
            .id(monitor.getId())
            .status(monitor.getStatus())
            .timeout(monitor.getTimeout())
            .timeRemaining(timeRemaining)
            .lastHeartbeat(monitor.getLastHeartbeat())
            .createdAt(monitor.getCreatedAt())
            .alertCount(monitor.getAlertCount())
            .alertEmail(monitor.getAlertEmail())
            .build();
    }
    
    @Override
    public MonitorResponse pauseMonitor(String deviceId) {
        log.info("Pausing monitor for device: {}", deviceId);
        
        Monitor monitor = monitorRepository.findById(deviceId)
            .orElseThrow(() -> new MonitorNotFoundException("Monitor not found for device: " + deviceId));
        
        if (monitor.getStatus() != MonitorStatus.ACTIVE) {
            throw new IllegalStateException("Only active monitors can be paused. Current status: " + monitor.getStatus());
        }
        
        // Cancel the timer
        timerService.cancelTimer(deviceId);
        
        // Update status
        monitorRepository.updateStatus(deviceId, MonitorStatus.PAUSED);
        
        auditService.log(deviceId, AuditAction.PAUSE, Map.of(
            "previousStatus", "ACTIVE"
        ));
        
        log.info("Monitor paused for device: {}", deviceId);
        
        return MonitorResponse.builder()
            .id(monitor.getId())
            .status(MonitorStatus.PAUSED)
            .message("Monitor paused successfully. Send heartbeat to resume.")
            .build();
    }
    
    @Override
    public MonitorResponse resumeMonitor(String deviceId) {
        log.info("Resuming monitor for device: {}", deviceId);
        
        Monitor monitor = monitorRepository.findById(deviceId)
            .orElseThrow(() -> new MonitorNotFoundException("Monitor not found for device: " + deviceId));
        
        if (monitor.getStatus() != MonitorStatus.PAUSED) {
            throw new IllegalStateException("Only paused monitors can be resumed. Current status: " + monitor.getStatus());
        }
        
        // Update status
        monitorRepository.updateStatus(deviceId, MonitorStatus.ACTIVE);
        
        // Update last heartbeat to now
        monitorRepository.updateLastHeartbeat(deviceId, LocalDateTime.now());
        
        // Start timer
        timerService.startTimer(deviceId, monitor.getTimeout());
        
        auditService.log(deviceId, AuditAction.RESUME, Map.of(
            "previousStatus", "PAUSED"
        ));
        
        log.info("Monitor resumed for device: {}", deviceId);
        
        return MonitorResponse.builder()
            .id(monitor.getId())
            .status(MonitorStatus.ACTIVE)
            .message("Monitor resumed successfully")
            .build();
    }
    
    @Override
    public void deleteMonitor(String deviceId) {
        log.info("Deleting monitor for device: {}", deviceId);
        
        if (!monitorRepository.existsById(deviceId)) {
            throw new MonitorNotFoundException(deviceId);
        }
        
        // Cancel timer
        timerService.cancelTimer(deviceId);
        
        // Delete from database
        monitorRepository.deleteById(deviceId);
        
        auditService.log(deviceId, AuditAction.DELETE, Map.of(
            "deletedAt", LocalDateTime.now().toString()
        ));
        
        log.info("Monitor deleted for device: {}", deviceId);
    }
    
    @Override
    public Page<MonitorResponse> getAllMonitors(String status, Pageable pageable) {
        Page<Monitor> monitors;
        
        if (status != null && !status.isEmpty()) {
            MonitorStatus monitorStatus = MonitorStatus.valueOf(status.toUpperCase());
            monitors = monitorRepository.findByStatus(monitorStatus, pageable);
        } else {
            monitors = monitorRepository.findAll(pageable);
        }
        
        return monitors.map(this::convertToResponse);
    }
    
    @Override
    public void markAsDown(String deviceId) {
        log.warn("Marking monitor as DOWN for device: {}", deviceId);
        
        monitorRepository.updateStatus(deviceId, MonitorStatus.DOWN);
        
        Monitor monitor = monitorRepository.findById(deviceId).orElse(null);
        if (monitor != null) {
            alertService.sendAlert(monitor);
        }
    }
    
    @Override
    public void processExpiredMonitor(String deviceId) {
        log.info("Processing expired monitor: {}", deviceId);
        
        Monitor monitor = monitorRepository.findById(deviceId).orElse(null);
        if (monitor != null && monitor.getStatus() == MonitorStatus.ACTIVE) {
            markAsDown(deviceId);
        }
    }
    
    private MonitorResponse convertToResponse(Monitor monitor) {
        return MonitorResponse.builder()
            .id(monitor.getId())
            .timeout(monitor.getTimeout())
            .status(monitor.getStatus())
            .alertEmail(monitor.getAlertEmail())
            .lastHeartbeat(monitor.getLastHeartbeat())
            .createdAt(monitor.getCreatedAt())
            .build();
    }
}