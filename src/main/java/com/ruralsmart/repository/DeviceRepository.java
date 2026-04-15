package com.ruralsmart.repository;

import com.ruralsmart.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer> {

    List<Device> findByDeviceType(String deviceType);

    List<Device> findByLocation(String location);

    List<Device> findBySensorType(String sensorType);

    Optional<Device> findByName(String name);

    @Query("SELECT d FROM Device d WHERE d.online = true")
    List<Device> findOnlineDevices();

    @Query("SELECT COUNT(d) FROM Device d WHERE d.deviceType = :deviceType AND d.online = true")
    Long countOnlineByType(@Param("deviceType") String deviceType);

    @Query("SELECT d FROM Device d WHERE d.sensorType IS NOT NULL")
    List<Device> findAllSensors();

    @Query("SELECT d FROM Device d WHERE d.deviceType = 'actuator'")
    List<Device> findAllActuators();
}