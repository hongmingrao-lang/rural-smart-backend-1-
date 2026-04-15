package com.ruralsmart.controller;//传感器api

import com.ruralsmart.dto.SensorDataDTO;
import com.ruralsmart.entity.SensorData;
import com.ruralsmart.service.SensorService;
import com.ruralsmart.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensor")
@CrossOrigin(origins = "*")
public class SensorController {

    @Autowired
    private SensorService sensorService;


    @PostMapping("/data")
    public Result<SensorData> receiveData(@Valid @RequestBody SensorDataDTO sensorDataDTO) {
        SensorData sensorData = sensorService.receiveData(sensorDataDTO);
        return Result.success(sensorData);
    }


    @GetMapping("/latest")
    public Result<List<SensorData>> getLatestData() {
        List<SensorData> latestData = sensorService.getLatestData();
        return Result.success(latestData);
    }


    @GetMapping("/device/{deviceId}")
    public Result<List<SensorData>> getDeviceData(@PathVariable Integer deviceId,
                                                  @RequestParam(required = false, defaultValue = "24") Integer hours) {
        List<SensorData> data = sensorService.getDeviceData(deviceId, hours);
        return Result.success(data);
    }


    @GetMapping("/type/{sensorType}")
    public Result<List<SensorData>> getSensorData(@PathVariable String sensorType,
                                                  @RequestParam(required = false, defaultValue = "24") Integer hours) {
        List<SensorData> data = sensorService.getSensorData(sensorType, hours);
        return Result.success(data);
    }


    @GetMapping("/type/{sensorType}/latest")
    public Result<SensorData> getLatestBySensorType(@PathVariable String sensorType) {
        SensorData data = sensorService.getLatestBySensorType(sensorType);
        return Result.success(data);
    }


    @GetMapping("/statistics")
    public Result<Map<String, Object>> getSensorStatistics(@RequestParam(required = false, defaultValue = "24") Integer hours) {
        Map<String, Object> statistics = sensorService.getSensorStatistics(hours);
        return Result.success(statistics);
    }
}