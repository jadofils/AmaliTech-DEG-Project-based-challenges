package com.watchdog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.watchdog.model.entity.Monitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Value("${alert.webhook.enabled:false}")
    private boolean webhookEnabled;

    @Value("${alert.webhook.url:}")
    private String defaultWebhookUrl;

    private static final int MAX_ALERTS_PER_HOUR = 3;
    private static final long CIRCUIT_BREAKER_COOLDOWN_MINUTES = 60;

    @Async
    public void sendAlert(Monitor monitor) {
        // Intelligent Alert Deduplication + Circuit Breaker
        if (!shouldSendAlert(monitor)) {
            log.warn("Circuit breaker OPEN for device: {} - alert suppressed to prevent fatigue", monitor.getId());
            return;
        }

        log.info("ALERT TRIGGERED for device: {}", monitor.getId());

        Map<String, Object> alert = buildAlertPayload(monitor);

        try {
            String alertJson = objectMapper.writeValueAsString(alert);
            log.error("ALERT: {}", alertJson);
        } catch (Exception e) {
            log.error("Failed to serialize alert", e);
        }

        if (webhookEnabled && defaultWebhookUrl != null && !defaultWebhookUrl.isEmpty()) {
            sendWebhook(monitor, alert);
        }

        simulateEmailAlert(monitor, alert);
    }

    private boolean shouldSendAlert(Monitor monitor) {
        int alertCount = monitor.getAlertCount();
        LocalDateTime lastAlertTime = monitor.getAlertTriggeredAt();

        // First failure: always alert immediately
        if (alertCount == 0) {
            log.debug("First alert for device: {} - sending immediately", monitor.getId());
            return true;
        }

        // If last alert was more than 1 hour ago, reset the circuit
        if (lastAlertTime != null) {
            long minutesSinceLastAlert = Duration.between(lastAlertTime, LocalDateTime.now()).toMinutes();
            if (minutesSinceLastAlert > CIRCUIT_BREAKER_COOLDOWN_MINUTES) {
                log.info("Circuit breaker RESET for device: {} - cooldown period passed", monitor.getId());
                return true;
            }
        }

        // Max 3 alerts per hour
        if (alertCount < MAX_ALERTS_PER_HOUR) {
            log.debug("Alert {} of {} for device: {} - sending with backoff", alertCount + 1, MAX_ALERTS_PER_HOUR, monitor.getId());
            return true;
        }

        // Circuit breaker engaged - suppress alerts
        log.warn("Circuit breaker ENGAGED for device: {} - too many alerts ({} in last hour)", monitor.getId(), alertCount);
        return false;
    }

    private Map<String, Object> buildAlertPayload(Monitor monitor) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("ALERT", String.format("Device %s is down!", monitor.getId()));
        alert.put("time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        alert.put("device_id", monitor.getId());
        alert.put("alert_email", monitor.getAlertEmail());
        alert.put("type", "timeout");
        alert.put("timeout_seconds", monitor.getTimeout());
        alert.put("last_heartbeat", monitor.getLastHeartbeat() != null ?
            monitor.getLastHeartbeat().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        alert.put("alert_count", monitor.getAlertCount() + 1);
        alert.put("circuit_breaker_status", monitor.getAlertCount() >= MAX_ALERTS_PER_HOUR ? "OPEN" : "CLOSED");
        return alert;
    }

    private void sendWebhook(Monitor monitor, Map<String, Object> alert) {
        String webhookUrl = monitor.getAlertWebhook() != null ?
            monitor.getAlertWebhook() : defaultWebhookUrl;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(alert, headers);
            restTemplate.postForEntity(webhookUrl, request, String.class);
            log.info("Webhook sent successfully for device: {}", monitor.getId());
        } catch (Exception e) {
            log.error("Failed to send webhook for device: {}", monitor.getId(), e);
        }
    }

    private void simulateEmailAlert(Monitor monitor, Map<String, Object> alert) {
        log.info("EMAIL (simulated) - To: {}, Subject: DEVICE ALERT: {} is DOWN",
            monitor.getAlertEmail(), monitor.getId());
        log.info("Email body: {}", alert);
    }
}
