package com.ruralsmart.impl;

import com.alibaba.fastjson.JSON;
import com.ruralsmart.dto.DeviceDTO;
import com.ruralsmart.entity.ControlLog;
import com.ruralsmart.entity.Device;
import com.ruralsmart.mqtt.MqttService;
import com.ruralsmart.repository.ControlLogRepository;
import com.ruralsmart.repository.DeviceRepository;
import com.ruralsmart.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ControlLogRepository controlLogRepository;

    @Autowired
    @Lazy
    private MqttService mqttService;

    @Value("${mqtt.topics.device-control:ruralsmart003}")
    private String defaultControlTopic;

    @Override
    @Transactional
    public Device addDevice(DeviceDTO deviceDTO) {
        Device device = new Device();
        BeanUtils.copyProperties(deviceDTO, device);
        device.setCreateTime(LocalDateTime.now());
        device.setUpdateTime(LocalDateTime.now());
        return deviceRepository.save(device);
    }

    @Override
    @Transactional
    public Device updateDevice(Integer id, DeviceDTO deviceDTO) {
        Optional<Device> optionalDevice = deviceRepository.findById(id);
        if (!optionalDevice.isPresent()) {
            throw new RuntimeException("设备不存在");
        }

        Device device = optionalDevice.get();
        device.setName(deviceDTO.getName());
        device.setDeviceType(deviceDTO.getDeviceType());
        device.setSensorType(deviceDTO.getSensorType());
        device.setLocation(deviceDTO.getLocation());
        device.setMqttTopic(deviceDTO.getMqttTopic());
        device.setStatus(deviceDTO.getStatus());
        device.setUpdateTime(LocalDateTime.now());

        return deviceRepository.save(device);
    }

    @Override
    @Transactional
    public void deleteDevice(Integer id) {
        if (!deviceRepository.existsById(id)) {
            throw new RuntimeException("设备不存在");
        }
        deviceRepository.deleteById(id);
    }

    @Override
    public Device getDeviceById(Integer id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("设备不存在"));
    }

    @Override
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    @Override
    public List<Device> getDevicesByType(String deviceType) {
        return deviceRepository.findByDeviceType(deviceType);
    }

    @Override
    public List<Device> getOnlineDevices() {
        return deviceRepository.findOnlineDevices();
    }

    @Override
    @Transactional
    public void updateDeviceStatus(Integer deviceId, Boolean online, String lastValue) {
        Optional<Device> optionalDevice = deviceRepository.findById(deviceId);
        if (optionalDevice.isPresent()) {
            Device device = optionalDevice.get();
            device.setOnline(online);
            device.setLastValue(lastValue);
            device.setLastSeen(LocalDateTime.now());
            device.setUpdateTime(LocalDateTime.now());
            deviceRepository.save(device);
        }
    }

    @Override
    public Map<String, Object> getDeviceStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalDevices = deviceRepository.count();
        long onlineDevices = deviceRepository.countOnlineByType("sensor") +
                deviceRepository.countOnlineByType("actuator");
        long sensorCount = deviceRepository.countOnlineByType("sensor");
        long actuatorCount = deviceRepository.countOnlineByType("actuator");

        stats.put("totalDevices", totalDevices);
        stats.put("onlineDevices", onlineDevices);
        stats.put("offlineDevices", totalDevices - onlineDevices);
        stats.put("sensorCount", sensorCount);
        stats.put("actuatorCount", actuatorCount);
        stats.put("onlineRate", totalDevices > 0 ? (onlineDevices * 100.0 / totalDevices) : 0);

        return stats;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void controlDevice(Integer deviceId, String command, String param) {
        Optional<Device> optionalDevice = deviceRepository.findById(deviceId);
        if (!optionalDevice.isPresent()) {
            throw new RuntimeException("设备不存在");
        }

        Device device = optionalDevice.get();
        String topic = device.getMqttTopic();

        // 如果设备未配置MQTT主题，使用默认控制主题
        if (topic == null || topic.isEmpty()) {
            topic = defaultControlTopic;
            log.warn("设备{}({})未配置MQTT主题，使用默认控制主题: {}", deviceId, device.getName(), topic);
        }

        // 构建控制消息
        Map<String, Object> message = new HashMap<>();
        message.put("deviceId", deviceId);
        message.put("command", command);
        message.put("param", param);
        message.put("timestamp", System.currentTimeMillis());

        try {
            // 发送MQTT消息
            mqttService.publish(topic, JSON.toJSONString(message));

            // 记录控制日志
            ControlLog log = new ControlLog();
            log.setDeviceId(deviceId);
            log.setDeviceName(device.getName());
            log.setCommand(command);
            log.setParam(param);
            log.setSource("manual");
            log.setResult("success");
            log.setMessage("控制指令发送成功");
            log.setCreateTime(LocalDateTime.now());
            controlLogRepository.save(log);

        } catch (Exception e) {

            ControlLog log = new ControlLog();
            log.setDeviceId(deviceId);
            log.setDeviceName(device.getName());
            log.setCommand(command);
            log.setParam(param);
            log.setSource("manual");
            log.setResult("failed");
            log.setMessage("控制指令发送失败: " + e.getMessage());
            log.setCreateTime(LocalDateTime.now());
            controlLogRepository.save(log);

            throw new RuntimeException("控制指令发送失败: " + e.getMessage());
        }
    }
}