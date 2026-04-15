package com.ruralsmart.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ControlRequest {

    @NotNull(message = "设备ID不能为空")
    private Integer deviceId;

    @NotBlank(message = "控制指令不能为空")
    private String command;  // on, off, set_temperature, set_mode

    private String param;  // JSON格式参数

    private String source = "manual";  // manual, automation, schedule
}