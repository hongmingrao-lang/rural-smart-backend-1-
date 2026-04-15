package com.ruralsmart.impl;

import com.ruralsmart.dto.SensorDataDTO;
import com.ruralsmart.entity.Device;
import com.ruralsmart.entity.SensorData;
import com.ruralsmart.repository.DeviceRepository;
import com.ruralsmart.repository.SensorDataRepository;
import com.ruralsmart.service.AutomationService;
import com.ruralsmart.service.SensorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SensorServiceImpl implements SensorService {

    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    @Lazy
    private AutomationService automationService;

    @Override
    @Transactional
    public SensorData receiveData(SensorDataDTO sensorDataDTO) {
        // 保存传感器数据
        SensorData sensorData = new SensorData();
        BeanUtils.copyProperties(sensorDataDTO, sensorData);
        sensorData.setCreateTime(LocalDateTime.now());
        SensorData savedData = sensorDataRepository.save(sensorData);

        log.info("传感器数据已保存: deviceId={}, type={}, value={}",
                sensorDataDTO.getDeviceId(), sensorDataDTO.getSensorType(), sensorDataDTO.getValue());

        // 更新设备状态
        Optional<Device> deviceOpt = deviceRepository.findById(sensorDataDTO.getDeviceId());
        if (deviceOpt.isPresent()) {
            Device device = deviceOpt.get();
            device.setOnline(true);
            device.setLastValue(String.valueOf(sensorDataDTO.getValue()));
            device.setLastSeen(LocalDateTime.now());
            deviceRepository.save(device);
        }

        // 将自动化检查注册到事务提交之后执行，确保传感器数据保存不受自动化处理影响
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    automationService.checkAllRules(sensorDataDTO);
                } catch (Exception e) {
                    log.error("自动化规则检查异常（不影响传感器数据）: {}", e.getMessage());
                }
            }
        });

        return savedData;
    }

    @Override
    public List<SensorData> getLatestData() {
        // 获取各类传感器的最新数据
        List<SensorData> latestData = new ArrayList<>();

        String[] sensorTypes = {"temperature", "humidity", "soil"};
        for (String sensorType : sensorTypes) {
            SensorData data = sensorDataRepository.findLatestBySensorType(sensorType);
            if (data != null) {
                latestData.add(data);
            }
        }

        return latestData;
    }

    @Override
    public List<SensorData> getDeviceData(Integer deviceId, Integer hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours != null ? hours : 24);
        return sensorDataRepository.findByDeviceIdAndTimeRange(deviceId, startTime);
    }

    @Override
    public List<SensorData> getSensorData(String sensorType, Integer hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours != null ? hours : 24);
        return sensorDataRepository.findBySensorTypeAndTimeRange(sensorType, startTime);
    }

    @Override
    public SensorData getLatestBySensorType(String sensorType) {
        return sensorDataRepository.findLatestBySensorType(sensorType);
    }

    @Override
    public Map<String, Object> getSensorStatistics(Integer hours) {
        hours = hours != null ? hours : 24;
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);

        Map<String, Object> stats = new HashMap<>();

        // 获取各类传感器的统计数据
        String[] sensorTypes = {"temperature", "humidity", "soil"};
        for (String sensorType : sensorTypes) {
            List<SensorData> dataList = sensorDataRepository.findBySensorTypeAndTimeRange(sensorType, startTime);
            if (!dataList.isEmpty()) {
                Map<String, Object> typeStats = new HashMap<>();

                // 最新值
                Double latestValue = dataList.get(0).getValue();
                typeStats.put("latest", latestValue);

                // 平均值
                Double average = dataList.stream()
                        .mapToDouble(SensorData::getValue)
                        .average()
                        .orElse(0.0);
                typeStats.put("average", average);

                // 最大值
                Double max = dataList.stream()
                        .mapToDouble(SensorData::getValue)
                        .max()
                        .orElse(0.0);
                typeStats.put("max", max);

                // 最小值
                Double min = dataList.stream()
                        .mapToDouble(SensorData::getValue)
                        .min()
                        .orElse(0.0);
                typeStats.put("min", min);

                // 数据点数量
                typeStats.put("count", dataList.size());

                stats.put(sensorType, typeStats);
            }
        }

        return stats;
    }

}