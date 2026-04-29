package com.watchdog.repository;

import com.watchdog.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByMonitorIdOrderByTimestampDesc(String monitorId);
    Page<AuditLog> findByMonitorId(String monitorId, Pageable pageable);
    List<AuditLog> findByAction(String action);

    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.monitorId = :monitorId")
    int deleteByMonitorId(@Param("monitorId") String monitorId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.action = :action")
    int deleteByAction(@Param("action") String action);

    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :cutoff")
    int deleteByTimestampBefore(@Param("cutoff") LocalDateTime cutoff);
}
