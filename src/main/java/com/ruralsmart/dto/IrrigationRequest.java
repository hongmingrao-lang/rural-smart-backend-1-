package com.ruralsmart.dto;

import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class IrrigationRequest {

    @NotNull(message = "区域ID不能为空")
    private Integer zoneId;

    private String zoneName;

    @NotNull(message = "设备ID不能为空")
    private Integer deviceId;

    @NotNull(message = "灌溉时长不能为空")
    @Min(value = 1, message = "灌溉时长必须大于0")
    private Integer duration;  // 秒

    private String mode = "manual";  // manual, auto, schedule
}