package com.ruralsmart.entity;//device表

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "device_type", nullable = false, length = 20)
    private String deviceType;  // sensor, actuator

    @Column(name = "sensor_type", length = 20)
    private String sensorType;  // temperature, humidity, soil, water_level

    @Column(name = "location", length = 50)
    private String location;  // living_room, bedroom, garden

    @Column(name = "mqtt_topic", length = 100)
    private String mqttTopic;

    @Column(name = "status")
    private Boolean status = true;  // 设备状态 true-启用, false-禁用

    @Column(name = "online")
    private Boolean online = false;  // 在线状态

    @Column(name = "last_data_value")
    private String lastValue;  // 最后上报的值

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;  // 最后在线时间

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "update_time")
    private LocalDateTime updateTime = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}