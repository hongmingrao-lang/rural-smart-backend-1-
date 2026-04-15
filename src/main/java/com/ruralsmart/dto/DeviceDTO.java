package com.ruralsmart.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class DeviceDTO {

    private Integer id;

    @NotBlank(message = "设备名称不能为空")
    private String name;

    @NotBlank(message = "设备类型不能为空")
    private String deviceType;  // sensor, actuator

    private String sensorType;  // temperature, humidity, soil

    private String location;  // living_room, bedroom, garden

    private String mqttTopic;

    private Boolean status = true;

    private Boolean online = false;

    private String lastValue;
}