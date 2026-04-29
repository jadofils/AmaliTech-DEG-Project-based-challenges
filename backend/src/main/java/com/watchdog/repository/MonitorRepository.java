package com.watchdog.repository;

import com.watchdog.model.entity.Monitor;
import com.watchdog.model.enums.MonitorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, String> {
    Page<Monitor> findByStatus(MonitorStatus status, Pageable pageable);
    List<Monitor> findByStatus(MonitorStatus status);
    List<Monitor> findByStatusAndUpdatedAtBefore(MonitorStatus status, LocalDateTime time);
}
