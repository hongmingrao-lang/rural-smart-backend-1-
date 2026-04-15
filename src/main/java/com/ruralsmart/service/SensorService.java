package com.ruralsmart.service;

import com.ruralsmart.dto.SensorDataDTO;
import com.ruralsmart.entity.SensorData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SensorService {

    // 数据接收
    SensorData receiveData(SensorDataDTO sensorDataDTO);

    // 数据查询
    List<SensorData> getLatestData();

    List<SensorData> getDeviceData(Integer deviceId, Integer hours);

    List<SensorData> getSensorData(String sensorType, Integer hours);

    SensorData getLatestBySensorType(String sensorType);

    Map<String, Object> getSensorStatistics(Integer hours);
}