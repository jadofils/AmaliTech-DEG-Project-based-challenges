package com.watchdog.controller;

import com.watchdog.dto.request.CreateMonitorRequest;
import com.watchdog.dto.response.HeartbeatResponse;
import com.watchdog.dto.response.MonitorResponse;
import com.watchdog.dto.response.StatusResponse;
import com.watchdog.service.MonitorService;
import com.watchdog.service.TimerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/monitors")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;
    private final TimerService timerService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "timestamp", LocalDateTime.now().toString(),
                "service", "watchdog-sentinel",
                "activeTimers", timerService.getActiveTimerCount(),
                "database", "connected"
        ));
    }

    @PostMapping
    public ResponseEntity<MonitorResponse> createMonitor(@Valid @RequestBody CreateMonitorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(monitorService.createMonitor(request));
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<HeartbeatResponse> heartbeat(@PathVariable String id) {
        return ResponseEntity.ok(monitorService.sendHeartbeat(id));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<StatusResponse> getStatus(@PathVariable String id) {
        return ResponseEntity.ok(monitorService.getStatus(id));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<MonitorResponse> pause(@PathVariable String id) {
        return ResponseEntity.ok(monitorService.pauseMonitor(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        monitorService.deleteMonitor(id);
        return ResponseEntity.ok(Map.of("message", "Monitor deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<Page<MonitorResponse>> getAllMonitors(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(monitorService.getAllMonitors(status, PageRequest.of(page, size, sort)));
    }
}
