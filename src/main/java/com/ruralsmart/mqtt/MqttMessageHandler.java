package com.ruralsmart.mqtt;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruralsmart.dto.SensorDataDTO;
import com.ruralsmart.service.DeviceService;
import com.ruralsmart.service.SensorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class MqttMessageHandler {

    @Autowired
    private SensorService sensorService;

    @Autowired
    private DeviceService deviceService;

    /**
     * 处理设备数据
     *
     * 支持两种格式:
     *
     * 格式1 - JSON (推荐):
     * {"deviceId":1,"sensorType":"temperature","value":25.5,"unit":"°C"}
     *
     * 格式2 - 巴法云简单格式 (用#分隔多个传感器值):
     * #25.5#60.2#
     * 第一个值=温度, 第二个值=湿度
     */
    public void handleDeviceData(String topic, String payload) {
        try {
            payload = payload.trim();

            if (payload.startsWith("{")) {
                // JSON 格式
                handleJsonData(payload);
            } else if (payload.startsWith("#")) {
                // 巴法云简单格式: #温度#湿度#
                handleBemfaData(payload);
            } else {
                // 尝试当作单个数值处理
                handleSimpleValue(payload);
            }

        } catch (Exception e) {
            log.error("处理设备数据失败: payload={}, error={}", payload, e.getMessage());
        }
    }

    /**
     * 处理 JSON 格式的传感器数据
     */
    private void handleJsonData(String payload) {
        JSONObject data = JSON.parseObject(payload);

        Integer deviceId = data.getInteger("deviceId");
        String deviceName = data.getString("deviceName");
        String sensorType = data.getString("sensorType");
        Double value = data.getDouble("value");
        String unit = data.getString("unit");
        String location = data.getString("location");

        SensorDataDTO sensorData = new SensorDataDTO();
        sensorData.setDeviceId(deviceId);
        sensorData.setDeviceName(deviceName);
        sensorData.setSensorType(sensorType);
        sensorData.setValue(value);
        sensorData.setUnit(unit);
        sensorData.setLocation(location);
        sensorData.setCreateTime(LocalDateTime.now());

        sensorService.receiveData(sensorData);
        log.info("收到JSON传感器数据: deviceId={}, type={}, value={}", deviceId, sensorType, value);
    }

    /**
     * 处理巴法云简单格式: #温度#湿度#土壤湿度#
     * STM32 通过 DHT11 采集后以此格式发送
     */
    private void handleBemfaData(String payload) {
        // 去掉首尾的 #，按 # 分割
        String cleaned = payload.replaceAll("^#+|#+$", "");
        String[] values = cleaned.split("#");

        log.info("解析巴法云数据: payload={}, 解析后values数量={}", payload, values.length);

        if (values.length >= 1) {
            // 第一个值: 温度
            Double temperature = parseDouble(values[0]);
            if (temperature != null) {
                saveSensorData(1, "温度传感器-A1", "temperature", temperature, "\u00B0C", "garden");
            }
        }

        if (values.length >= 2) {
            // 第二个值: 湿度
            Double humidity = parseDouble(values[1]);
            if (humidity != null) {
                saveSensorData(2, "湿度传感器-A2", "humidity", humidity, "%", "garden");
            }
        }

        if (values.length >= 3) {
            // 第三个值: 土壤湿度
            Double soilMoisture = parseDouble(values[2]);
            log.info("土壤湿度解析: raw='{}', parsed={}", values[2], soilMoisture);
            if (soilMoisture != null) {
                saveSensorData(3, "土壤湿度传感器-B1", "soil", soilMoisture, "%", "garden");
            }
        } else {
            log.warn("巴法云数据缺少土壤湿度值，payload={}，values数量={}（需要至少3个值）", payload, values.length);
        }

        log.info("收到巴法云传感器数据: {}", payload);
    }

    /**
     * 处理单个数值
     */
    private void handleSimpleValue(String payload) {
        Double value = parseDouble(payload);
        if (value != null) {
            saveSensorData(1, "温度传感器-A1", "temperature", value, "\u00B0C", "garden");
            log.info("收到简单数值: {}", value);
        }
    }

    /**
     * 保存传感器数据到数据库
     */
    private void saveSensorData(Integer deviceId, String deviceName, String sensorType,
                                 Double value, String unit, String location) {
        SensorDataDTO dto = new SensorDataDTO();
        dto.setDeviceId(deviceId);
        dto.setDeviceName(deviceName);
        dto.setSensorType(sensorType);
        dto.setValue(value);
        dto.setUnit(unit);
        dto.setLocation(location);
        dto.setCreateTime(LocalDateTime.now());
        sensorService.receiveData(dto);
    }

    private Double parseDouble(String str) {
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 处理设备状态
     *
     * JSON格式: {"deviceId":1,"online":true,"lastValue":"25.5"}
     */
    public void handleDeviceStatus(String topic, String payload) {
        try {
            payload = payload.trim();

            if (payload.startsWith("{")) {
                JSONObject status = JSON.parseObject(payload);
                Integer deviceId = status.getInteger("deviceId");
                Boolean online = status.getBoolean("online");
                String lastValue = status.getString("lastValue");
                deviceService.updateDeviceStatus(deviceId, online, lastValue);
                log.info("更新设备状态: deviceId={}, online={}", deviceId, online);
            } else {
                // 简单格式: "online" 或 "offline"
                log.info("收到设备状态消息: {}", payload);
            }

        } catch (Exception e) {
            log.error("处理设备状态失败: {}", e.getMessage());
        }
    }

    /**
     * 处理控制响应
     *
     * JSON格式: {"deviceId":1,"command":"on","success":true,"message":"已开启"}
     */
    public void handleControlResponse(String topic, String payload) {
        try {
            payload = payload.trim();

            if (payload.startsWith("{")) {
                JSONObject response = JSON.parseObject(payload);
                Integer deviceId = response.getInteger("deviceId");
                String command = response.getString("command");
                Boolean success = response.getBoolean("success");
                String message = response.getString("message");

                log.info("设备控制响应: deviceId={}, command={}, success={}, message={}",
                        deviceId, command, success, message);
            } else {
                // 巴法云简单响应: "on" / "off"
                log.info("收到控制响应: {}", payload);
            }

        } catch (Exception e) {
            log.error("处理控制响应失败: {}", e.getMessage());
        }
    }
}