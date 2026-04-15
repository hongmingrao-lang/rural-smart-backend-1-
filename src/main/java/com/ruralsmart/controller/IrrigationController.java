package com.ruralsmart.controller;//灌溉api

import com.ruralsmart.dto.IrrigationRequest;
import com.ruralsmart.entity.IrrigationLog;
import com.ruralsmart.service.IrrigationService;
import com.ruralsmart.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/irrigation")
@CrossOrigin(origins = "*")
public class IrrigationController {

    @Autowired
    private IrrigationService irrigationService;


    @PostMapping("/start")
    public Result<Void> startIrrigation(@Valid @RequestBody IrrigationRequest request) {
        irrigationService.startIrrigation(request);
        return Result.success();
    }


    @PostMapping("/stop/{zoneId}")
    public Result<Void> stopIrrigation(@PathVariable Integer zoneId) {
        irrigationService.stopIrrigation(zoneId);
        return Result.success();
    }


    @GetMapping("/logs")
    public Result<List<IrrigationLog>> getIrrigationLogs(@RequestParam(required = false) Integer zoneId,
                                                         @RequestParam(required = false) String mode,
                                                         @RequestParam(required = false, defaultValue = "7") Integer days) {
        List<IrrigationLog> logs = irrigationService.getIrrigationLogs(zoneId, mode, days);
        return Result.success(logs);
    }


    @GetMapping("/statistics")
    public Result<Map<String, Object>> getIrrigationStatistics(@RequestParam(required = false, defaultValue = "7") Integer days) {
        Map<String, Object> statistics = irrigationService.getIrrigationStatistics(days);
        return Result.success(statistics);
    }


    @PostMapping("/check")
    public Result<Void> checkAndIrrigate() {
        irrigationService.checkAndIrrigate();
        return Result.success();
    }
}