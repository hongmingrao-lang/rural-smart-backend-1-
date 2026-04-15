package com.ruralsmart.repository;

import com.ruralsmart.entity.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, Integer> {

    List<AutomationRule> findByEnabledTrue();

    List<AutomationRule> findByConditionType(String conditionType);

    List<AutomationRule> findByActionType(String actionType);

    @Query("SELECT a FROM AutomationRule a WHERE a.enabled = true ORDER BY a.createTime DESC")
    List<AutomationRule> findActiveRules();
}