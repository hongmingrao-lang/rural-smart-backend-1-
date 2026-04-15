package com.ruralsmart.service;

import com.ruralsmart.dto.IrrigationRequest;
import com.ruralsmart.entity.IrrigationLog;
import java.util.List;
import java.util.Map;

public interface IrrigationService {

    // 启动灌溉
    void startIrrigation(IrrigationRequest request);

    // 停止灌溉
    void stopIrrigation(Integer zoneId);

    // 查询灌溉记录
    List<IrrigationLog> getIrrigationLogs(Integer zoneId, String mode, Integer days);

    // 获取灌溉统计
    Map<String, Object> getIrrigationStatistics(Integer days);

    // 自动灌溉检查
    void checkAndIrrigate();
}