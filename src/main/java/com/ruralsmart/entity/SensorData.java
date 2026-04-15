package com.ruralsmart.entity;//传感器表

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Integer deviceId;

    @Column(name = "device_name", length = 50)
    private String deviceName;

    @Column(name = "sensor_type", nullable = false, length = 20)
    private String sensorType;  // temperature, humidity, soil_moisture

    @Column(name = "value", nullable = false, precision = 8, scale = 2)
    private Double value;

    @Column(name = "unit", length = 10)
    private String unit;  // °C, %, %

    @Column(name = "location", length = 50)
    private String location;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}