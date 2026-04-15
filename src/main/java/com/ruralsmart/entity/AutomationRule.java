package com.ruralsmart.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "automation_rule")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "condition_type", length = 20)
    private String conditionType;  // sensor, time, combination

    @Column(name = "condition_config", columnDefinition = "TEXT")
    private String conditionConfig;  // JSON格式的条件配置

    @Column(name = "action_type", length = 20)
    private String actionType;  // device_control, irrigation, notification

    @Column(name = "action_config", columnDefinition = "TEXT")
    private String actionConfig;  // JSON格式的动作配置

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "last_triggered")
    private LocalDateTime lastTriggered;

    @Column(name = "trigger_count")
    private Integer triggerCount = 0;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "update_time")
    private LocalDateTime updateTime = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}