package com.watchdog.service.impl;

import com.watchdog.dto.request.CreateMonitorRequest;
import com.watchdog.dto.response.HeartbeatResponse;
import com.watchdog.dto.response.MonitorResponse;
import com.watchdog.dto.response.StatusResponse;
import com.watchdog.exception.MonitorExpiredException;
import com.watchdog.exception.MonitorNotFoundException;
import com.watchdog.model.entity.Monitor;
import com.watchdog.model.enums.MonitorStatus;
import com.watchdog.repository.MonitorRepository;
import com.watchdog.service.MonitorService;
import com.watchdog.service.TimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    private final MonitorRepository monitorRepository;
    private final TimerService timerService;

    @Override
    @Transactional
    public MonitorResponse createMonitor(CreateMonitorRequest request) {
        if (monitorRepository.existsById(request.getId())) {
            throw new IllegalStateException("Monitor already exists for device: " + request.getId());
        }
        Monitor monitor = Monitor.builder()
                .id(request.getId())
                .timeout(request.getTimeout())
                .alertEmail(request.getAlertEmail())
                .alertWebhook(request.getAlertWebhook())
                .status(MonitorStatus.ACTIVE)
                .alertCount(0)
                .build();
        monitorRepository.save(monitor);
        timerService.startTimer(monitor.getId(), monitor.getTimeout());
        log.info("Monitor created: {} (timeout: {}s)", monitor.getId(), monitor.getTimeout());
        return toResponse(monitor, "Monitor created successfully");
    }

    @Override
    @Transactional
    public HeartbeatResponse sendHeartbeat(String id) {
        Monitor monitor = monitorRepository.findById(id)
                .orElseThrow(() -> new MonitorNotFoundException(id));

        if (monitor.getStatus() == MonitorStatus.DOWN) {
            throw new MonitorExpiredException(id);
        }

        boolean resumed = monitor.getStatus() == MonitorStatus.PAUSED;
        monitor.setStatus(MonitorStatus.ACTIVE);
        monitor.setLastHeartbeat(LocalDateTime.now());
        monitorRepository.save(monitor);
        timerService.resetTimer(id, monitor.getTimeout());

        log.info("Heartbeat received: {}, timer reset{}",id, resumed ? " (resumed)" : "");
        return HeartbeatResponse.builder()
                .deviceId(id)
                .message(resumed ? "Heartbeat received - monitor resumed" : "Heartbeat received - timer reset")
                .timeRemaining(monitor.getTimeout())
                .resumed(resumed)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public StatusResponse getStatus(String id) {
        Monitor monitor = monitorRepository.findById(id)
                .orElseThrow(() -> new MonitorNotFoundException(id));
        long timeRemaining = timerService.getTimeRemaining(id, monitor.getTimeout());
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
    @Transactional
    public MonitorResponse pauseMonitor(String id) {
        Monitor monitor = monitorRepository.findById(id)
                .orElseThrow(() -> new MonitorNotFoundException(id));
        if (monitor.getStatus() == MonitorStatus.DOWN) {
            throw new MonitorExpiredException(id);
        }
        monitor.setStatus(MonitorStatus.PAUSED);
        monitorRepository.save(monitor);
        timerService.cancelTimer(id);
        log.info("Monitor paused: {}", id);
        return toResponse(monitor, "Monitor paused. Send a heartbeat to auto-resume.");
    }

    @Override
    @Transactional
    public void deleteMonitor(String id) {
        if (!monitorRepository.existsById(id)) {
            throw new MonitorNotFoundException(id);
        }
        timerService.cancelTimer(id);
        monitorRepository.deleteById(id);
        log.info("Monitor deleted: {}", id);
    }

    @Override
    public Page<MonitorResponse> getAllMonitors(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            MonitorStatus monitorStatus = MonitorStatus.valueOf(status.toUpperCase());
            return monitorRepository.findByStatus(monitorStatus, pageable).map(m -> toResponse(m, null));
        }
        return monitorRepository.findAll(pageable).map(m -> toResponse(m, null));
    }

    private MonitorResponse toResponse(Monitor monitor, String message) {
        return MonitorResponse.builder()
                .id(monitor.getId())
                .timeout(monitor.getTimeout())
                .status(monitor.getStatus())
                .alertEmail(monitor.getAlertEmail())
                .alertWebhook(monitor.getAlertWebhook())
                .lastHeartbeat(monitor.getLastHeartbeat())
                .createdAt(monitor.getCreatedAt())
                .alertCount(monitor.getAlertCount())
                .message(message)
                .build();
    }
}
