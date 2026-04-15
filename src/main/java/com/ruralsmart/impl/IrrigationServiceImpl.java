package com.ruralsmart.impl;

import com.alibaba.fastjson.JSON;
import com.ruralsmart.dto.IrrigationRequest;
import com.ruralsmart.entity.Device;
import com.ruralsmart.entity.IrrigationLog;
import com.ruralsmart.mqtt.MqttService;
import com.ruralsmart.repository.DeviceRepository;
import com.ruralsmart.repository.IrrigationLogRepository;
import com.ruralsmart.service.IrrigationService;
import com.ruralsmart.service.SensorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class IrrigationServiceImpl implements IrrigationService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private IrrigationLogRepository irrigationLogRepository;

    @Autowired
    private MqttService mqttService;

    @Autowired
    private SensorService sensorService;

    @Value("${rural.automation.soil-moisture-threshold:30}")
    private Double soilMoistureThreshold;

    @Value("${rural.automation.irrigation-duration:600}")
    private Integer defaultIrrigationDuration;

    @Value("${mqtt.topics.device-control:ruralsmart003}")
    private String defaultControlTopic;

    // 正在进行的灌溉任务
    private final Map<Integer, IrrigationTask> activeIrrigationTasks = new ConcurrentHashMap<>();

    // 共享定时线程池，避免每次 new Timer() 造成线程泄漏
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startIrrigation(IrrigationRequest request) {
        Optional<Device> deviceOpt = deviceRepository.findById(request.getDeviceId());
        if (!deviceOpt.isPresent()) {
            throw new RuntimeException("灌溉设备不存在");
        }

        Device device = deviceOpt.get();
        if (!"actuator".equals(device.getDeviceType())) {
            throw new RuntimeException("设备不是执行器类型");
        }

        // 获取当前土壤湿度
        Double currentSoilMoisture = getCurrentSoilMoisture(request.getZoneId());

        // 创建灌溉记录
        IrrigationLog irrigationLog = new IrrigationLog();
        irrigationLog.setZoneId(request.getZoneId());
        irrigationLog.setZoneName(request.getZoneName());
        irrigationLog.setDeviceId(request.getDeviceId());
        irrigationLog.setDuration(request.getDuration());
        irrigationLog.setSoilMoistureBefore(currentSoilMoisture);
        irrigationLog.setMode(request.getMode());
        irrigationLog.setStatus("running");
        irrigationLog.setCreateTime(LocalDateTime.now());

        IrrigationLog savedLog = irrigationLogRepository.save(irrigationLog);

        // 发送MQTT控制指令
        Map<String, Object> controlMsg = new HashMap<>();
        controlMsg.put("zoneId", request.getZoneId());
        controlMsg.put("deviceId", request.getDeviceId());
        controlMsg.put("command", "start");
        controlMsg.put("duration", request.getDuration());
        controlMsg.put("logId", savedLog.getId());

        try {
            String topic = device.getMqttTopic() != null && !device.getMqttTopic().isEmpty()
                    ? device.getMqttTopic() : defaultControlTopic;
            log.info("灌溉控制使用MQTT主题: {}, 设备: {}({})", topic, device.getId(), device.getName());
            mqttService.publish(topic, JSON.toJSONString(controlMsg));

            // 启动定时任务，在灌溉结束后更新状态
            IrrigationTask task = new IrrigationTask(savedLog.getId(), request.getDuration());
            activeIrrigationTasks.put(request.getZoneId(), task);

            scheduleIrrigationCompletion(savedLog.getId(), request.getDuration());

            log.info("启动灌溉: zoneId={}, duration={}s, mode={}",
                    request.getZoneId(), request.getDuration(), request.getMode());

        } catch (Exception e) {
            irrigationLog.setStatus("failed");
            irrigationLogRepository.save(irrigationLog);
            throw new RuntimeException("启动灌溉失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void stopIrrigation(Integer zoneId) {
        IrrigationTask task = activeIrrigationTasks.get(zoneId);
        if (task == null) {
            throw new RuntimeException("该区域没有正在进行的灌溉任务");
        }

        // 发送停止指令
        Optional<IrrigationLog> logOpt = irrigationLogRepository.findById(task.getLogId());
        if (logOpt.isPresent()) {
            IrrigationLog irrigationLog = logOpt.get();

            Optional<Device> deviceOpt = deviceRepository.findById(irrigationLog.getDeviceId());
            if (deviceOpt.isPresent()) {
                Device device = deviceOpt.get();

                Map<String, Object> controlMsg = new HashMap<>();
                controlMsg.put("zoneId", zoneId);
                controlMsg.put("deviceId", irrigationLog.getDeviceId());
                controlMsg.put("command", "stop");

                String topic = device.getMqttTopic() != null && !device.getMqttTopic().isEmpty()
                        ? device.getMqttTopic() : defaultControlTopic;
                log.info("停止灌溉使用MQTT主题: {}, 设备: {}({})", topic, device.getId(), device.getName());
                mqttService.publish(topic, JSON.toJSONString(controlMsg));

                // 更新灌溉记录
                irrigationLog.setStatus("stopped");
                irrigationLog.setEndTime(LocalDateTime.now());
                irrigationLogRepository.save(irrigationLog);

                // 移除任务
                activeIrrigationTasks.remove(zoneId);

                log.info("停止灌溉: zoneId={}", zoneId);
            }
        }
    }

    @Override
    public List<IrrigationLog> getIrrigationLogs(Integer zoneId, String mode, Integer days) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days != null ? days : 7);

        if (zoneId != null) {
            return irrigationLogRepository.findByZoneIdOrderByCreateTimeDesc(zoneId);
        } else if (mode != null) {
            return irrigationLogRepository.findByMode(mode);
        } else {
            return irrigationLogRepository.findRecentLogs(startTime);
        }
    }

    @Override
    public Map<String, Object> getIrrigationStatistics(Integer days) {
        days = days != null ? days : 7;
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);

        Map<String, Object> stats = new HashMap<>();

        // 获取统计数据
        List<IrrigationLog> recentLogs = irrigationLogRepository.findRecentLogs(startTime);

        long totalIrrigations = recentLogs.size();
        long completedIrrigations = recentLogs.stream()
                .filter(log -> "completed".equals(log.getStatus()))
                .count();

        Long totalDuration = irrigationLogRepository.getTotalIrrigationTime(startTime);
        Double totalWaterUsage = irrigationLogRepository.getTotalWaterUsage(startTime);

        // 按模式统计
        Map<String, Long> modeStats = new HashMap<>();
        for (IrrigationLog log : recentLogs) {
            String mode = log.getMode();
            modeStats.put(mode, modeStats.getOrDefault(mode, 0L) + 1);
        }

        // 按区域统计
        Map<Integer, Long> zoneStats = new HashMap<>();
        for (IrrigationLog log : recentLogs) {
            Integer zoneId = log.getZoneId();
            zoneStats.put(zoneId, zoneStats.getOrDefault(zoneId, 0L) + 1);
        }

        stats.put("totalIrrigations", totalIrrigations);
        stats.put("completedIrrigations", completedIrrigations);
        stats.put("totalDuration", totalDuration != null ? totalDuration : 0);
        stats.put("totalWaterUsage", totalWaterUsage != null ? totalWaterUsage : 0.0);
        stats.put("averageDuration", totalIrrigations > 0 ? (totalDuration != null ? totalDuration / totalIrrigations : 0) : 0);
        stats.put("modeStats", modeStats);
        stats.put("zoneStats", zoneStats);

        return stats;
    }

    @Override
    @Scheduled(fixedDelay = 60000) // 每分钟检查一次
    public void checkAndIrrigate() {
        log.debug("开始自动灌溉检查");

        // 这里可以添加自动灌溉逻辑
        // 例如：检查所有区域的土壤湿度，如果低于阈值则自动灌溉

        // 获取所有土壤湿度传感器
        List<Device> soilSensors = deviceRepository.findBySensorType("soil");

        for (Device sensor : soilSensors) {
            // 获取最新土壤湿度数据
            Double currentMoisture = getCurrentSoilMoistureByDevice(sensor.getId());

            if (currentMoisture != null && currentMoisture < soilMoistureThreshold) {
                log.info("区域{}土壤湿度{}%低于阈值{}%，触发自动灌溉",
                        sensor.getLocation(), currentMoisture, soilMoistureThreshold);

                // 自动启动灌溉
                IrrigationRequest request = new IrrigationRequest();
                request.setZoneId(1); // 需要根据实际情况设置区域ID
                request.setZoneName(sensor.getLocation() + "区域");
                request.setDeviceId(findPumpDeviceId(sensor.getLocation()));
                request.setDuration(defaultIrrigationDuration);
                request.setMode("auto");

                try {
                    startIrrigation(request);
                } catch (Exception e) {
                    log.error("自动灌溉失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 获取当前土壤湿度
     */
    private Double getCurrentSoilMoisture(Integer zoneId) {
        // 根据区域ID找到对应的土壤湿度传感器
        // 这里简化处理，实际应根据区域和设备关联关系查询
        List<Device> soilSensors = deviceRepository.findBySensorType("soil");
        if (!soilSensors.isEmpty()) {
            return getCurrentSoilMoistureByDevice(soilSensors.get(0).getId());
        }
        return null;
    }

    private Double getCurrentSoilMoistureByDevice(Integer deviceId) {
        // 从传感器服务获取该设备最新的土壤湿度数据
        com.ruralsmart.entity.SensorData latestData = sensorService.getLatestBySensorType("soil");
        if (latestData != null) {
            return latestData.getValue();
        }
        return null;
    }

    /**
     * 根据位置找到水泵设备ID
     */
    private Integer findPumpDeviceId(String location) {
        List<Device> actuators = deviceRepository.findByDeviceType("actuator");
        for (Device actuator : actuators) {
            if (actuator.getName().contains("水泵") || actuator.getName().contains("pump")) {
                return actuator.getId();
            }
        }
        return 1; // 默认返回第一个设备
    }

    /**
     * 安排灌溉完成后的处理
     */
    private void scheduleIrrigationCompletion(Long logId, Integer duration) {
        scheduler.schedule(() -> {
            try {
                Optional<IrrigationLog> logOpt = irrigationLogRepository.findById(logId);
                if (logOpt.isPresent()) {
                    IrrigationLog irrigationLog = logOpt.get();
                    if ("running".equals(irrigationLog.getStatus())) {
                        irrigationLog.setStatus("completed");
                        irrigationLog.setEndTime(LocalDateTime.now());

                        // 获取灌溉后的土壤湿度
                        Double afterMoisture = getCurrentSoilMoisture(irrigationLog.getZoneId());
                        irrigationLog.setSoilMoistureAfter(afterMoisture);

                        // 计算用水量（假设5升/分钟）
                        double waterConsumption = 5.0 * duration / 60.0;
                        irrigationLog.setWaterVolume(waterConsumption);

                        irrigationLogRepository.save(irrigationLog);

                        // 从活动任务中移除
                        activeIrrigationTasks.remove(irrigationLog.getZoneId());

                        log.info("灌溉完成: logId={}, zoneId={}", logId, irrigationLog.getZoneId());
                    }
                }
            } catch (Exception e) {
                log.error("处理灌溉完成任务失败: logId={}, error={}", logId, e.getMessage());
            }
        }, duration, TimeUnit.SECONDS);
    }

    /**
     * 灌溉任务内部类
     */
    private static class IrrigationTask {
        private Long logId;
        private Integer duration;
        private LocalDateTime startTime;

        public IrrigationTask(Long logId, Integer duration) {
            this.logId = logId;
            this.duration = duration;
            this.startTime = LocalDateTime.now();
        }

        public Long getLogId() {
            return logId;
        }

        public Integer getDuration() {
            return duration;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }
    }
}