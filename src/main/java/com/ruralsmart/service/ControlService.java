package com.ruralsmart.service;

import com.ruralsmart.dto.ControlRequest;
import com.ruralsmart.entity.ControlLog;
import java.util.List;
import java.util.Map;

public interface ControlService {

    // 发送控制指令
    void sendControlCommand(ControlRequest request);

    // 查询控制记录
    List<ControlLog> getControlLogs(Integer deviceId, String source, Integer hours);

    // 获取控制统计
    Map<String, Object> getControlStatistics(Integer hours);
}