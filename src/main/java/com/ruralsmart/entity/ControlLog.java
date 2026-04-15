package com.ruralsmart.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "control_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ControlLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Integer deviceId;

    @Column(name = "device_name", length = 50)
    private String deviceName;

    @Column(name = "command", nullable = false, length = 50)
    private String command;  // on, off, set_temperature

    @Column(name = "param", length = 100)
    private String param;  // JSON参数

    @Column(name = "source", length = 20)
    private String source;  // manual, automation, schedule

    @Column(name = "result", length = 20)
    private String result;  // success, failed, timeout

    @Column(name = "message", length = 200)
    private String message;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}