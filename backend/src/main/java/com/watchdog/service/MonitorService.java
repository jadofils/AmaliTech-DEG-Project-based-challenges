package com.watchdog.service;

import com.watchdog.dto.request.CreateMonitorRequest;
import com.watchdog.dto.response.HeartbeatResponse;
import com.watchdog.dto.response.MonitorResponse;
import com.watchdog.dto.response.StatusResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MonitorService {
    MonitorResponse createMonitor(CreateMonitorRequest request);
    HeartbeatResponse sendHeartbeat(String id);
    StatusResponse getStatus(String id);
    MonitorResponse pauseMonitor(String id);
    void deleteMonitor(String id);
    Page<MonitorResponse> getAllMonitors(String status, Pageable pageable);
}
