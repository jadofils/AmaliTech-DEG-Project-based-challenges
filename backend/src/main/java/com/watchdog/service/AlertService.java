package com.watchdog.service;

import com.watchdog.model.entity.Monitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AlertService {

    @Value("${alert.webhook.enabled:false}")
    private boolean webhookEnabled;

    @Value("${alert.webhook.url:}")
    private String webhookUrl;

    public void sendAlert(Monitor monitor) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ALERT", "Device " + monitor.getId() + " is down!");
        payload.put("time", LocalDateTime.now().toString());
        payload.put("deviceId", monitor.getId());
        payload.put("alertEmail", monitor.getAlertEmail());
        payload.put("alertCount", monitor.getAlertCount());

        log.error("ALERT: {}", payload);
        log.info("EMAIL simulated: To: {}, Subject: Device {} is down!", monitor.getAlertEmail(), monitor.getId());

        if (webhookEnabled && webhookUrl != null && !webhookUrl.isBlank()) {
            log.info("Webhook dispatched to: {}", webhookUrl);
        }
    }
}
