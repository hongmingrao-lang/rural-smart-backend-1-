package com.ruralsmart.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruralsmart.dto.AutomationRuleDTO;
import com.ruralsmart.dto.IrrigationRequest;
import com.ruralsmart.dto.SensorDataDTO;
import com.ruralsmart.entity.AutomationRule;
import com.ruralsmart.repository.AutomationRuleRepository;
import com.ruralsmart.service.AutomationService;
import com.ruralsmart.service.ControlService;
import com.ruralsmart.service.DeviceService;
import com.ruralsmart.service.IrrigationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class AutomationServiceImpl implements AutomationService {

    @Autowired
    private AutomationRuleRepository automationRuleRepository;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private ControlService controlService;

    @Autowired
    private IrrigationService irrigationService;

    @Value("${rural.automation.temperature-threshold:28}")
    private Double temperatureThreshold;

    @Value("${rural.automation.soil-moisture-threshold:30}")
    private Double soilMoistureThreshold;

    @Value("${rural.automation.irrigation-duration:600}")
    private Integer irrigationDuration;

    @Override
    @Transactional
    public AutomationRule createRule(AutomationRuleDTO ruleDTO) {
        // 验证条件配置
        validateConditionConfig(ruleDTO.getConditionConfig());

        // 验证动作配置
        validateActionConfig(ruleDTO.getActionConfig());

        AutomationRule rule = new AutomationRule();
        BeanUtils.copyProperties(ruleDTO, rule);
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        rule.setTriggerCount(0);

        return automationRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public AutomationRule updateRule(Integer id, AutomationRuleDTO ruleDTO) {
        Optional<AutomationRule> ruleOpt = automationRuleRepository.findById(id);
        if (!ruleOpt.isPresent()) {
            throw new RuntimeException("自动化规则不存在");
        }

        // 验证条件配置
        validateConditionConfig(ruleDTO.getConditionConfig());

        // 验证动作配置
        validateActionConfig(ruleDTO.getActionConfig());

        AutomationRule rule = ruleOpt.get();
        rule.setName(ruleDTO.getName());
        rule.setDescription(ruleDTO.getDescription());
        rule.setConditionType(ruleDTO.getConditionType());
        rule.setConditionConfig(ruleDTO.getConditionConfig());
        rule.setActionType(ruleDTO.getActionType());
        rule.setActionConfig(ruleDTO.getActionConfig());
        rule.setEnabled(ruleDTO.getEnabled());
        rule.setUpdateTime(LocalDateTime.now());

        return automationRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public void deleteRule(Integer id) {
        if (!automationRuleRepository.existsById(id)) {
            throw new RuntimeException("自动化规则不存在");
        }
        automationRuleRepository.deleteById(id);
    }

    @Override
    public AutomationRule getRuleById(Integer id) {
        return automationRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("自动化规则不存在"));
    }

    @Override
    public List<AutomationRule> getAllRules() {
        return automationRuleRepository.findAll();
    }

    @Override
    public List<AutomationRule> getActiveRules() {
        return automationRuleRepository.findActiveRules();
    }

    @Override
    public void triggerTemperatureControl(Double temperature) {
        log.info("触发温度控制自动化: temperature={}°C", temperature);

        // 查找空调设备
        List<com.ruralsmart.entity.Device> devices = deviceService.getDevicesByType("actuator");
        for (com.ruralsmart.entity.Device device : devices) {
            if (device.getName().contains("空调") || device.getName().contains("air")) {
                // 发送控制指令
                Map<String, Object> param = new HashMap<>();
                param.put("mode", "cool");
                param.put("temperature", 26);

                com.ruralsmart.dto.ControlRequest request = new com.ruralsmart.dto.ControlRequest();
                request.setDeviceId(device.getId());
                request.setCommand("on");
                request.setParam(JSON.toJSONString(param));
                request.setSource("automation");

                try {
                    controlService.sendControlCommand(request);
                    log.info("已自动打开空调");
                } catch (Exception e) {
                    log.error("自动打开空调失败: {}", e.getMessage());
                }
                break;
            }
        }
    }

    @Override
    public void triggerIrrigation(Double soilMoisture) {
        log.info("触发灌溉自动化: soilMoisture={}%", soilMoisture);

        // 动态查找水泵设备
        List<com.ruralsmart.entity.Device> actuators = deviceService.getDevicesByType("actuator");
        Integer pumpDeviceId = null;
        for (com.ruralsmart.entity.Device device : actuators) {
            if (device.getName().contains("水泵") || device.getName().contains("pump")) {
                pumpDeviceId = device.getId();
                break;
            }
        }

        if (pumpDeviceId == null) {
            log.warn("未找到水泵设备，跳过自动灌溉");
            return;
        }

        // 创建灌溉请求
        com.ruralsmart.dto.IrrigationRequest request = new com.ruralsmart.dto.IrrigationRequest();
        request.setZoneId(1);
        request.setZoneName("自动灌溉区域");
        request.setDeviceId(pumpDeviceId);
        request.setDuration(irrigationDuration);
        request.setMode("auto");

        try {
            irrigationService.startIrrigation(request);
            log.info("已自动启动灌溉，时长{}秒", irrigationDuration);
        } catch (Exception e) {
            log.error("自动灌溉失败: {}", e.getMessage());
        }
    }

    @Override
    public void checkAllRules(SensorDataDTO sensorData) {
        List<AutomationRule> activeRules = getActiveRules();

        for (AutomationRule rule : activeRules) {
            if (evaluateCondition(rule, sensorData)) {
                executeAction(rule, sensorData);

                // 更新规则触发信息
                rule.setLastTriggered(LocalDateTime.now());
                rule.setTriggerCount(rule.getTriggerCount() + 1);
                automationRuleRepository.save(rule);

                log.info("自动化规则触发: {}", rule.getName());
            }
        }
    }

    @Override
    public Map<String, Object> getRuleStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<AutomationRule> allRules = getAllRules();
        List<AutomationRule> activeRules = getActiveRules();

        long totalRules = allRules.size();
        long activeCount = activeRules.size();
        long triggeredCount = allRules.stream()
                .filter(rule -> rule.getTriggerCount() > 0)
                .count();

        // 按类型统计
        Map<String, Long> typeStats = new HashMap<>();
        for (AutomationRule rule : allRules) {
            String type = rule.getActionType();
            typeStats.put(type, typeStats.getOrDefault(type, 0L) + 1);
        }

        stats.put("totalRules", totalRules);
        stats.put("activeRules", activeCount);
        stats.put("inactiveRules", totalRules - activeCount);
        stats.put("triggeredRules", triggeredCount);
        stats.put("typeStats", typeStats);

        return stats;
    }


    private void validateConditionConfig(String conditionConfig) {
        try {
            JSON.parseObject(conditionConfig);
        } catch (Exception e) {
            throw new RuntimeException("条件配置JSON格式错误: " + e.getMessage());
        }
    }


    private void validateActionConfig(String actionConfig) {
        try {
            JSON.parseObject(actionConfig);
        } catch (Exception e) {
            throw new RuntimeException("动作配置JSON格式错误: " + e.getMessage());
        }
    }


    private boolean evaluateCondition(AutomationRule rule, SensorDataDTO sensorData) {
        try {
            JSONObject condition = JSON.parseObject(rule.getConditionConfig());
            String conditionType = condition.getString("type");

            // 兼容没有 type 字段的旧数据：如果包含 sensorType 则视为 sensor_value
            if (conditionType == null || conditionType.isEmpty()) {
                if (condition.containsKey("sensorType")) {
                    conditionType = "sensor_value";
                } else {
                    log.warn("条件配置缺少type字段: {}", rule.getConditionConfig());
                    return false;
                }
            }

            switch (conditionType) {
                case "sensor_value":
                    return evaluateSensorCondition(condition, sensorData);
                case "time_range":
                    return evaluateTimeCondition(condition);
                case "and":
                    return evaluateAndCondition(condition, sensorData);
                case "or":
                    return evaluateOrCondition(condition, sensorData);
                default:
                    log.warn("未知的条件类型: {}", conditionType);
                    return false;
            }
        } catch (Exception e) {
            log.error("评估条件时出错: {}", e.getMessage());
            return false;
        }
    }


    private boolean evaluateSensorCondition(JSONObject condition, SensorDataDTO sensorData) {
        String sensorType = condition.getString("sensorType");
        String operator = condition.getString("operator");
        Double threshold = condition.getDouble("threshold");

        if (!sensorType.equals(sensorData.getSensorType())) {
            return false;
        }

        Double value = sensorData.getValue();

        switch (operator) {
            case "gt":  // greater than
            case ">":
                return value > threshold;
            case "gte": // greater than or equal
            case ">=":
                return value >= threshold;
            case "lt":  // less than
            case "<":
                return value < threshold;
            case "lte": // less than or equal
            case "<=":
                return value <= threshold;
            case "eq":  // equal
            case "==":
                return Math.abs(value - threshold) < 0.001;
            default:
                return false;
        }
    }


    private boolean evaluateTimeCondition(JSONObject condition) {
        return true;
    }


    private boolean evaluateAndCondition(JSONObject condition, SensorDataDTO sensorData) {
        List<JSONObject> conditions = condition.getJSONArray("conditions").toJavaList(JSONObject.class);
        for (JSONObject cond : conditions) {
            if (!evaluateCondition(new AutomationRule() {{
                setConditionConfig(cond.toJSONString());
            }}, sensorData)) {
                return false;
            }
        }
        return true;
    }


    private boolean evaluateOrCondition(JSONObject condition, SensorDataDTO sensorData) {
        List<JSONObject> conditions = condition.getJSONArray("conditions").toJavaList(JSONObject.class);
        for (JSONObject cond : conditions) {
            if (evaluateCondition(new AutomationRule() {{
                setConditionConfig(cond.toJSONString());
            }}, sensorData)) {
                return true;
            }
        }
        return false;
    }


    private void executeAction(AutomationRule rule, SensorDataDTO sensorData) {
        try {
            JSONObject action = JSON.parseObject(rule.getActionConfig());
            String actionType = action.getString("type");

            // 兼容没有 type 字段的旧数据：根据 actionType 字段或内容推断
            if (actionType == null || actionType.isEmpty()) {
                actionType = rule.getActionType();
            }

            switch (actionType) {
                case "device_control":
                    executeDeviceControl(action);
                    break;
                case "irrigation":
                    executeIrrigation(action);
                    break;
                case "notification":
                    executeNotification(action, sensorData);
                    break;
                default:
                    log.warn("未知的动作类型: {}", actionType);
            }
        } catch (Exception e) {
            log.error("执行动作时出错: {}", e.getMessage());
        }
    }


    private void executeDeviceControl(JSONObject action) {
        Integer deviceId = action.getInteger("deviceId");
        String command = action.getString("command");
        String param = action.getString("param");

        com.ruralsmart.dto.ControlRequest request = new com.ruralsmart.dto.ControlRequest();
        request.setDeviceId(deviceId);
        request.setCommand(command);
        request.setParam(param);
        request.setSource("automation");

        controlService.sendControlCommand(request);
    }

    private void executeIrrigation(JSONObject action) {
        Integer deviceId = action.getInteger("deviceId");

        // 如果规则配置中没有 deviceId，动态查找水泵设备
        if (deviceId == null) {
            List<com.ruralsmart.entity.Device> actuators = deviceService.getDevicesByType("actuator");
            for (com.ruralsmart.entity.Device device : actuators) {
                if (device.getName().contains("水泵") || device.getName().contains("pump")) {
                    deviceId = device.getId();
                    break;
                }
            }
        }

        if (deviceId == null) {
            log.warn("executeIrrigation: 未找到水泵设备，跳过灌溉动作");
            return;
        }

        IrrigationRequest request = new IrrigationRequest();
        request.setZoneId(action.getInteger("zoneId"));
        request.setZoneName(action.getString("zoneName"));
        request.setDeviceId(deviceId);
        request.setDuration(action.getInteger("duration"));
        request.setMode("auto");

        irrigationService.startIrrigation(request);
    }


    private void executeNotification(JSONObject action, SensorDataDTO sensorData) {
        String message = action.getString("message");
        // 这里可以集成邮件、短信、微信等通知方式
        log.info("发送通知: {}", message);
    }
}