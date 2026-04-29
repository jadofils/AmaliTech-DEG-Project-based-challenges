// controller/MonitorController.java
package com.watchdog.controller;

import com.watchdog.dto.request.CreateMonitorRequest;
import com.watchdog.dto.response.HeartbeatResponse;
import com.watchdog.dto.response.MonitorResponse;
import com.watchdog.dto.response.StatusResponse;
import com.watchdog.service.MonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.watchdog.service.TimerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/monitors")
@RequiredArgsConstructor
@Tag(name = "Monitor Management", description = "Dead Man's Switch API — register devices, send heartbeats, and receive alerts when a device stops responding. Each monitor starts a countdown timer. If no heartbeat is received before the timer expires, an alert is fired automatically. Features intelligent alert deduplication with a circuit breaker to prevent alert fatigue in unstable connectivity environments.")
public class MonitorController {
    
    private final MonitorService monitorService;
    private final TimerService timerService;
    
    @PostMapping
    @Operation(
        summary = "Create a new monitor",
        description = "Registers a new device with a countdown timer. Once created, the timer starts immediately. If no heartbeat is received within the timeout period, an alert is triggered. Device ID must be unique."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Monitor created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Monitor already exists")
    })
    public ResponseEntity<MonitorResponse> createMonitor(@Valid @RequestBody CreateMonitorRequest request) {
        MonitorResponse response = monitorService.createMonitor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/{id}/heartbeat")
    @Operation(
        summary = "Send heartbeat",
        description = "Resets the countdown timer for an active device. Call this endpoint periodically from your device to prove it is still alive. If the monitor is PAUSED, sending a heartbeat will automatically resume it. If the monitor is DOWN, a 410 is returned and you must re-register."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Heartbeat received - timer reset"),
        @ApiResponse(responseCode = "404", description = "Monitor not found"),
        @ApiResponse(responseCode = "410", description = "Monitor expired"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<HeartbeatResponse> sendHeartbeat(
            @Parameter(description = "Device ID", required = true)
            @PathVariable String id) {
        HeartbeatResponse response = monitorService.sendHeartbeat(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}/status")
    @Operation(
        summary = "Get monitor status",
        description = "Returns the current state of a monitor. Possible states:\n\n" +
            "**ACTIVE** - The device is being monitored. The countdown timer is running. Every heartbeat resets it back to the full timeout. If no heartbeat arrives before the timer hits zero, the system automatically transitions to DOWN and fires an alert.\n\n" +
            "**PAUSED** - The timer is stopped. No alerts will fire regardless of how long the device stays silent. This is used during planned maintenance. The monitor stays PAUSED until a heartbeat is received, which auto-resumes it back to ACTIVE.\n\n" +
            "**DOWN** - The timer expired with no heartbeat received. An alert has already been fired. The device needs attention. You cannot send heartbeats to a DOWN monitor - you must delete it and re-register to start monitoring again."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Monitor not found")
    })
    public ResponseEntity<StatusResponse> getMonitorStatus(
            @Parameter(description = "Device ID", required = true)
            @PathVariable String id) {
        StatusResponse response = monitorService.getMonitorStatus(id);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/pause")
    @Operation(
        summary = "Pause monitoring",
        description = "Stops the countdown timer without triggering an alert. Use this during planned maintenance or device repairs. The monitor stays PAUSED until a heartbeat is received, which auto-resumes it."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Monitor paused successfully"),
        @ApiResponse(responseCode = "404", description = "Monitor not found"),
        @ApiResponse(responseCode = "409", description = "Monitor cannot be paused in current state")
    })
    public ResponseEntity<MonitorResponse> pauseMonitor(
            @Parameter(description = "Device ID", required = true)
            @PathVariable String id) {
        MonitorResponse response = monitorService.pauseMonitor(id);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/resume")
    @Operation(
        summary = "Resume monitoring",
        description = "Manually resumes a PAUSED monitor and restarts the countdown timer. Note: sending a heartbeat also resumes automatically — this endpoint is for manual control."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Monitor resumed successfully"),
        @ApiResponse(responseCode = "404", description = "Monitor not found"),
        @ApiResponse(responseCode = "409", description = "Monitor cannot be resumed in current state")
    })
    public ResponseEntity<MonitorResponse> resumeMonitor(
            @Parameter(description = "Device ID", required = true)
            @PathVariable String id) {
        MonitorResponse response = monitorService.resumeMonitor(id);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete monitor",
        description = "Permanently removes a monitor and cancels its timer. Use this when a device is decommissioned. This action cannot be undone — you must re-register the device to monitor it again."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Monitor deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Monitor not found")
    })
    public ResponseEntity<Map<String, String>> deleteMonitor(
            @Parameter(description = "Device ID", required = true)
            @PathVariable String id) {
        monitorService.deleteMonitor(id);
        return ResponseEntity.ok(Map.of("message", "Monitor deleted successfully"));
    }
    
    @GetMapping
    @Operation(
        summary = "List all monitors",
        description = "Returns a paginated list of all monitors. Filter by status (ACTIVE, PAUSED, DOWN). Sort by any field. Useful for dashboards and bulk status checks."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Monitors retrieved successfully")
    })
    public ResponseEntity<Page<MonitorResponse>> getAllMonitors(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<MonitorResponse> monitors = monitorService.getAllMonitors(status, pageable);
        return ResponseEntity.ok(monitors);
    }
    
    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Returns the current health of the API including database connectivity and number of active timers running in memory."
    )
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "timestamp", java.time.LocalDateTime.now(),
            "service", "watchdog-sentinel",
            "activeTimers", timerService.getActiveTimerCount(),
            "database", "connected"
        ));
    }
}