package com.ruralsmart.impl;

import com.ruralsmart.dto.ControlRequest;
import com.ruralsmart.entity.ControlLog;
import com.ruralsmart.repository.ControlLogRepository;
import com.ruralsmart.repository.DeviceRepository;
import com.ruralsmart.service.ControlService;
import com.ruralsmart.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ControlServiceImpl implements ControlService {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ControlLogRepository controlLogRepository;

    @Override
    @Transactional
    public void sendControlCommand(ControlRequest request) {
        // 调用设备服务的控制方法（DeviceServiceImpl 内部已记录控制日志）
        deviceService.controlDevice(request.getDeviceId(), request.getCommand(), request.getParam());

        log.info("控制指令已发送: deviceId={}, command={}, source={}",
                request.getDeviceId(), request.getCommand(), request.getSource());
    }

    @Override
    public List<ControlLog> getControlLogs(Integer deviceId, String source, Integer hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours != null ? hours : 24);

        if (deviceId != null) {
            return controlLogRepository.findByDeviceIdOrderByCreateTimeDesc(deviceId);
        } else if (source != null) {
            return controlLogRepository.findBySource(source);
        } else {
            return controlLogRepository.findRecentLogs(startTime);
        }
    }

    @Override
    public Map<String, Object> getControlStatistics(Integer hours) {
        hours = hours != null ? hours : 24;
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);

        Map<String, Object> stats = new HashMap<>();

        // 获取最近的控制记录
        List<ControlLog> recentLogs = controlLogRepository.findRecentLogs(startTime);

        long totalControls = recentLogs.size();
        long successControls = recentLogs.stream()
                .filter(log -> "success".equals(log.getResult()))
                .count();
        long failedControls = totalControls - successControls;

        // 按设备类型统计
        Map<String, Long> deviceControls = new HashMap<>();
        for (ControlLog log : recentLogs) {
            String deviceName = log.getDeviceName();
            deviceControls.put(deviceName, deviceControls.getOrDefault(deviceName, 0L) + 1);
        }

        // 按来源统计
        Map<String, Long> sourceControls = new HashMap<>();
        for (ControlLog log : recentLogs) {
            String source = log.getSource();
            sourceControls.put(source, sourceControls.getOrDefault(source, 0L) + 1);
        }

        stats.put("totalControls", totalControls);
        stats.put("successControls", successControls);
        stats.put("failedControls", failedControls);
        stats.put("successRate", totalControls > 0 ? (successControls * 100.0 / totalControls) : 0);
        stats.put("deviceControls", deviceControls);
        stats.put("sourceControls", sourceControls);

        return stats;
    }
}