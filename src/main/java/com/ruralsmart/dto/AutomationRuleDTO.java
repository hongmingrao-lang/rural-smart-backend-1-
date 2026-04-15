package com.ruralsmart.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AutomationRuleDTO {

    private Integer id;

    @NotBlank(message = "规则名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "条件类型不能为空")
    private String conditionType;  // sensor, time, combination

    @NotBlank(message = "条件配置不能为空")
    private String conditionConfig;  // JSON格式

    @NotBlank(message = "动作类型不能为空")
    private String actionType;  // device_control, irrigation, notification

    @NotBlank(message = "动作配置不能为空")
    private String actionConfig;  // JSON格式

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled = true;
}