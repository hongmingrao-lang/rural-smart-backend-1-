package com.ruralsmart.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "irrigation_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IrrigationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_id", nullable = false)
    private Integer zoneId;  // 灌溉区域

    @Column(name = "zone_name", length = 50)
    private String zoneName;  // 区域名称

    @Column(name = "device_id")
    private Integer deviceId;  // 水泵设备ID

    @Column(name = "duration", nullable = false)
    private Integer duration;  // 灌溉时长(秒)

    @Column(name = "water_volume", precision = 8, scale = 2)
    private Double waterVolume;  // 用水量(L)

    @Column(name = "soil_moisture_before", precision = 5, scale = 2)
    private Double soilMoistureBefore;  // 灌溉前土壤湿度

    @Column(name = "soil_moisture_after", precision = 5, scale = 2)
    private Double soilMoistureAfter;  // 灌溉后土壤湿度

    @Column(name = "mode", length = 20)
    private String mode;  // manual, auto, schedule

    @Column(name = "status", length = 20)
    private String status;  // running, completed, failed

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "end_time")
    private LocalDateTime endTime;
}