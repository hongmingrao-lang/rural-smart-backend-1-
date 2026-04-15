package com.ruralsmart.controller;//自动化api

import com.ruralsmart.dto.AutomationRuleDTO;
import com.ruralsmart.entity.AutomationRule;
import com.ruralsmart.service.AutomationService;
import com.ruralsmart.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/automation")
@CrossOrigin(origins = "*")
public class AutomationController {

    @Autowired
    private AutomationService automationService;


    @GetMapping("/rules")
    public Result<List<AutomationRule>> getAllRules() {
        List<AutomationRule> rules = automationService.getAllRules();
        return Result.success(rules);
    }


    @GetMapping("/rules/active")
    public Result<List<AutomationRule>> getActiveRules() {
        List<AutomationRule> rules = automationService.getActiveRules();
        return Result.success(rules);
    }


    @GetMapping("/rules/{id}")
    public Result<AutomationRule> getRule(@PathVariable Integer id) {
        AutomationRule rule = automationService.getRuleById(id);
        return Result.success(rule);
    }


    @PostMapping("/rules")
    public Result<AutomationRule> createRule(@Valid @RequestBody AutomationRuleDTO ruleDTO) {
        AutomationRule rule = automationService.createRule(ruleDTO);
        return Result.success(rule);
    }


    @PutMapping("/rules/{id}")
    public Result<AutomationRule> updateRule(@PathVariable Integer id,
                                             @Valid @RequestBody AutomationRuleDTO ruleDTO) {
        AutomationRule rule = automationService.updateRule(id, ruleDTO);
        return Result.success(rule);
    }


    @DeleteMapping("/rules/{id}")
    public Result<Void> deleteRule(@PathVariable Integer id) {
        automationService.deleteRule(id);
        return Result.success();
    }


    @GetMapping("/statistics")
    public Result<Map<String, Object>> getRuleStatistics() {
        Map<String, Object> statistics = automationService.getRuleStatistics();
        return Result.success(statistics);
    }
}