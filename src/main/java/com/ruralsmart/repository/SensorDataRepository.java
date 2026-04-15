package com.ruralsmart.repository;

import com.ruralsmart.entity.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    List<SensorData> findByDeviceIdOrderByCreateTimeDesc(Integer deviceId);

    List<SensorData> findBySensorTypeOrderByCreateTimeDesc(String sensorType);

    @Query("SELECT s FROM SensorData s WHERE s.deviceId = :deviceId AND s.createTime >= :startTime ORDER BY s.createTime DESC")
    List<SensorData> findByDeviceIdAndTimeRange(@Param("deviceId") Integer deviceId,
                                                @Param("startTime") LocalDateTime startTime);

    @Query("SELECT s FROM SensorData s WHERE s.sensorType = :sensorType AND s.createTime >= :startTime ORDER BY s.createTime DESC")
    List<SensorData> findBySensorTypeAndTimeRange(@Param("sensorType") String sensorType,
                                                  @Param("startTime") LocalDateTime startTime);

    // 修改1：使用原生查询
    @Query(value = "SELECT * FROM sensor_data WHERE sensor_type = :sensorType ORDER BY create_time DESC LIMIT 1", nativeQuery = true)
    SensorData findLatestBySensorType(@Param("sensorType") String sensorType);

    // 修改2：使用原生查询
    @Query(value = "SELECT * FROM sensor_data WHERE device_id = :deviceId ORDER BY create_time DESC LIMIT 1", nativeQuery = true)
    SensorData findLatestByDeviceId(@Param("deviceId") Integer deviceId);

    @Query("SELECT AVG(s.value) FROM SensorData s WHERE s.deviceId = :deviceId AND s.createTime >= :startTime")
    Double getAverageValue(@Param("deviceId") Integer deviceId,
                           @Param("startTime") LocalDateTime startTime);
}