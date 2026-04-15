package com.ruralsmart.service;

import com.ruralsmart.dto.AutomationRuleDTO;
import com.ruralsmart.dto.SensorDataDTO;
import com.ruralsmart.entity.AutomationRule;
import java.util.List;
import java.util.Map;

public interface AutomationService {

    // 规则管理
    AutomationRule createRule(AutomationRuleDTO ruleDTO);

    AutomationRule updateRule(Integer id, AutomationRuleDTO ruleDTO);

    void deleteRule(Integer id);

    AutomationRule getRuleById(Integer id);

    List<AutomationRule> getAllRules();

    List<AutomationRule> getActiveRules();

    // 触发规则
    void triggerTemperatureControl(Double temperature);

    void triggerIrrigation(Double soilMoisture);

    void checkAllRules(SensorDataDTO sensorData);

    // 统计
    Map<String, Object> getRuleStatistics();
}