package com.ruralsmart.mqtt;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class MqttService {

    @Autowired
    private MqttClient mqttClient;

    @Autowired
    private MqttMessageHandler messageHandler;

    @Value("${mqtt.qos:1}")
    private int qos;

    @Value("${mqtt.topics.device-data}")
    private String deviceDataTopic;

    @Value("${mqtt.topics.device-status}")
    private String deviceStatusTopic;

    @Value("${mqtt.topics.device-control}")
    private String deviceControlTopic;

    @PostConstruct
    public void init() {
        subscribeTopics();
    }


    private void subscribeTopics() {
        try {
            // 订阅传感器数据主题
            mqttClient.subscribe(deviceDataTopic, qos, (topic, message) -> {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                log.debug("收到传感器数据: topic={}, payload={}", topic, payload);
                messageHandler.handleDeviceData(topic, payload);
            });

            // 订阅设备状态主题
            mqttClient.subscribe(deviceStatusTopic, qos, (topic, message) -> {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                log.debug("收到设备状态: topic={}, payload={}", topic, payload);
                messageHandler.handleDeviceStatus(topic, payload);
            });

            // 订阅控制响应主题（巴法云不支持通配符，精确订阅）
            mqttClient.subscribe(deviceControlTopic, qos, (topic, message) -> {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                log.debug("收到控制响应: topic={}, payload={}", topic, payload);
                messageHandler.handleControlResponse(topic, payload);
            });

            log.info("MQTT主题订阅完成: data={}, status={}, control={}",
                    deviceDataTopic, deviceStatusTopic, deviceControlTopic);
        } catch (MqttException e) {
            log.error("MQTT订阅失败: {}", e.getMessage());
        }
    }


    public void publish(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
            mqttMessage.setQos(qos);
            mqttMessage.setRetained(false);
            mqttClient.publish(topic, mqttMessage);
            log.debug("发布MQTT消息: topic={}, message={}", topic, message);
        } catch (MqttException e) {
            log.error("MQTT发布失败: topic={}, error={}", topic, e.getMessage());
            throw new RuntimeException("MQTT发布失败: " + e.getMessage());
        }
    }

    /**
     * 向控制主题发布指令，STM32 订阅此主题接收控制命令
     */
    public void publishControl(String message) {
        publish(deviceControlTopic, message);
    }


    public void publishJson(String topic, Object message) {
        publish(topic, JSON.toJSONString(message));
    }


    @PreDestroy
    public void disconnect() {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
                log.info("MQTT连接已断开");
            }
            mqttClient.close();
            log.info("MQTT客户端资源已释放");
        } catch (MqttException e) {
            log.error("MQTT断开连接失败: {}", e.getMessage());
        }
    }
}