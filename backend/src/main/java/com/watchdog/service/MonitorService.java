// service/MonitorService.java
package com.watchdog.service;

import com.watchdog.dto.request.CreateMonitorRequest;
import com.watchdog.dto.response.HeartbeatResponse;
import com.watchdog.dto.response.MonitorResponse;
import com.watchdog.dto.response.StatusResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MonitorService {
    MonitorResponse createMonitor(CreateMonitorRequest request);
    HeartbeatResponse sendHeartbeat(String deviceId);
    StatusResponse getMonitorStatus(String deviceId);
    MonitorResponse pauseMonitor(String deviceId);
    MonitorResponse resumeMonitor(String deviceId);
    void deleteMonitor(String deviceId);
    Page<MonitorResponse> getAllMonitors(String status, Pageable pageable);
    void markAsDown(String deviceId);
    void processExpiredMonitor(String deviceId);
}