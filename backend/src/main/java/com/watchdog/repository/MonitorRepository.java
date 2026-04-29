// repository/MonitorRepository.java
package com.watchdog.repository;

import com.watchdog.model.entity.Monitor;
import com.watchdog.model.enums.MonitorStatus;
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
import java.util.Optional;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, String> {
    
    Optional<Monitor> findByIdAndStatusNot(String id, MonitorStatus status);
    
    Page<Monitor> findByStatus(MonitorStatus status, Pageable pageable);
    
    @Modifying
    @Transactional
    @Query("UPDATE Monitor m SET m.lastHeartbeat = :heartbeatTime WHERE m.id = :id")
    int updateLastHeartbeat(@Param("id") String id, 
                           @Param("heartbeatTime") LocalDateTime heartbeatTime);
    
    @Modifying
    @Transactional
    @Query("UPDATE Monitor m SET m.status = :status WHERE m.id = :id")
    int updateStatus(@Param("id") String id, 
                    @Param("status") MonitorStatus status);
    
    @Modifying
    @Transactional
    @Query("UPDATE Monitor m SET m.alertCount = m.alertCount + 1, " +
           "m.alertTriggeredAt = :alertTime WHERE m.id = :id")
    int incrementAlertCount(@Param("id") String id, 
                           @Param("alertTime") LocalDateTime alertTime);
    
    List<Monitor> findByStatusAndLastHeartbeatBefore(MonitorStatus status, 
                                                     LocalDateTime cutoffTime);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Monitor m WHERE m.status = :status AND m.updatedAt < :cutoffTime")
    int deleteOldMonitors(@Param("status") MonitorStatus status, 
                         @Param("cutoffTime") LocalDateTime cutoffTime);
    
    boolean existsByIdAndStatus(String id, MonitorStatus status);
}