package com.watchdog.model.entity;

import com.watchdog.model.enums.MonitorStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "monitors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Monitor {

    @Id
    @Column(nullable = false, unique = true)
    private String id;

    @Column(nullable = false)
    private int timeout;

    @Column(name = "alert_email", nullable = false)
    private String alertEmail;

    @Column(name = "alert_webhook")
    private String alertWebhook;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonitorStatus status;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "alert_triggered_at")
    private LocalDateTime alertTriggeredAt;

    @Column(name = "alert_count")
    private int alertCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastHeartbeat = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
