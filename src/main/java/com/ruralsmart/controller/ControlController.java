package com.ruralsmart.controller;//设备控制api

import com.ruralsmart.dto.ControlRequest;
import com.ruralsmart.entity.ControlLog;
import com.ruralsmart.service.ControlService;
import com.ruralsmart.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/control")
@CrossOrigin(origins = "*")
public class ControlController {

    @Autowired
    private ControlService controlService;


    @PostMapping
    public Result<Void> sendControl(@Valid @RequestBody ControlRequest request) {
        controlService.sendControlCommand(request);
        return Result.success();
    }


    @GetMapping("/logs")
    public Result<List<ControlLog>> getControlLogs(@RequestParam(required = false) Integer deviceId,
                                                   @RequestParam(required = false) String source,
                                                   @RequestParam(required = false, defaultValue = "24") Integer hours) {
        List<ControlLog> logs = controlService.getControlLogs(deviceId, source, hours);
        return Result.success(logs);
    }


    @GetMapping("/statistics")
    public Result<Map<String, Object>> getControlStatistics(@RequestParam(required = false, defaultValue = "24") Integer hours) {
        Map<String, Object> statistics = controlService.getControlStatistics(hours);
        return Result.success(statistics);
    }
}