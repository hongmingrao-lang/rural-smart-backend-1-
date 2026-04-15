package com.ruralsmart.repository;

import com.ruralsmart.entity.ControlLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ControlLogRepository extends JpaRepository<ControlLog, Long> {

    List<ControlLog> findByDeviceIdOrderByCreateTimeDesc(Integer deviceId);

    List<ControlLog> findByCommand(String command);

    List<ControlLog> findBySource(String source);

    @Query("SELECT c FROM ControlLog c WHERE c.createTime >= :startTime ORDER BY c.createTime DESC")
    List<ControlLog> findRecentLogs(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT COUNT(c) FROM ControlLog c WHERE c.deviceId = :deviceId AND c.result = 'success'")
    Long countSuccessfulControls(@Param("deviceId") Integer deviceId);
}