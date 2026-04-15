package com.ruralsmart.config;//链接mqtt

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@Slf4j
public class MqttConfig {

    @Value("${mqtt.broker}")
    private String broker;

    @Value("${mqtt.private-key:}")
    private String privateKey;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    @Value("${mqtt.qos:1}")
    private int qos;

    @Value("${mqtt.timeout:10}")
    private int timeout;

    @Value("${mqtt.keepalive:60}")
    private int keepalive;

    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        // 巴法云不需要 username/password，通过 clientId=私钥 来鉴权
        if (username != null && !username.isEmpty()) {
            options.setUserName(username);
        }
        if (password != null && !password.isEmpty()) {
            options.setPassword(password.toCharArray());
        }
        options.setConnectionTimeout(timeout);
        options.setKeepAliveInterval(keepalive);
        options.setCleanSession(true);
        options.setAutomaticReconnect(false);
        return options;
    }

    @Bean
    public MqttClient mqttClient(MqttConnectOptions options) throws MqttException {
        String actualClientId;
        if (privateKey != null && !privateKey.isEmpty()) {
            actualClientId = privateKey;
        } else {
            actualClientId = clientId;
        }
        log.info("连接MQTT Broker: {}, clientId: {}...", broker, actualClientId.substring(0, Math.min(8, actualClientId.length())) + "***");
        MqttClient client = new MqttClient(broker, actualClientId, new MemoryPersistence());
        client.connect(options);
        log.info("MQTT Broker 连接成功");
        return client;
    }
}