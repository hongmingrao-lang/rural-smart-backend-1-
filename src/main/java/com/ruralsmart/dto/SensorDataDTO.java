package com.ruralsmart.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class SensorDataDTO {

    @NotNull(message = "设备ID不能为空")
    private Integer deviceId;

    private String deviceName;

    @NotNull(message = "传感器类型不能为空")
    private String sensorType;  // temperature, humidity, soil_moisture

    @NotNull(message = "传感器值不能为空")
    private Double value;

    private String unit;  // °C, %, %

    private String location;

    private LocalDateTime createTime = LocalDateTime.now();
}