package com.ruralsmart.repository;

import com.ruralsmart.entity.IrrigationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IrrigationLogRepository extends JpaRepository<IrrigationLog, Long> {

    List<IrrigationLog> findByZoneIdOrderByCreateTimeDesc(Integer zoneId);

    List<IrrigationLog> findByMode(String mode);

    @Query("SELECT i FROM IrrigationLog i WHERE i.createTime >= :startTime ORDER BY i.createTime DESC")
    List<IrrigationLog> findRecentLogs(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT SUM(i.duration) FROM IrrigationLog i WHERE i.createTime >= :startTime")
    Long getTotalIrrigationTime(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT SUM(i.waterVolume) FROM IrrigationLog i WHERE i.createTime >= :startTime")
    Double getTotalWaterUsage(@Param("startTime") LocalDateTime startTime);
}