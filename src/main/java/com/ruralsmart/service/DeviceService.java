package com.ruralsmart.service;

import com.ruralsmart.dto.DeviceDTO;
import com.ruralsmart.entity.Device;
import java.util.List;
import java.util.Map;

public interface DeviceService {

    // 设备管理
    Device addDevice(DeviceDTO deviceDTO);

    Device updateDevice(Integer id, DeviceDTO deviceDTO);

    void deleteDevice(Integer id);

    Device getDeviceById(Integer id);

    List<Device> getAllDevices();

    List<Device> getDevicesByType(String deviceType);

    List<Device> getOnlineDevices();

    // 设备状态
    void updateDeviceStatus(Integer deviceId, Boolean online, String lastValue);

    Map<String, Object> getDeviceStatistics();

    // 设备控制
    void controlDevice(Integer deviceId, String command, String param);
}