package com.ruralsmart.controller;//设备api接口

import com.ruralsmart.dto.DeviceDTO;
import com.ruralsmart.entity.Device;
import com.ruralsmart.service.DeviceService;
import com.ruralsmart.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@CrossOrigin(origins = "*")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;


    @GetMapping
    public Result<List<Device>> getAllDevices() {
        List<Device> devices = deviceService.getAllDevices();
        return Result.success(devices);
    }


    @GetMapping("/online")
    public Result<List<Device>> getOnlineDevices() {
        List<Device> devices = deviceService.getOnlineDevices();
        return Result.success(devices);
    }


    @GetMapping("/statistics")
    public Result<Map<String, Object>> getDeviceStatistics() {
        Map<String, Object> statistics = deviceService.getDeviceStatistics();
        return Result.success(statistics);
    }


    @GetMapping("/type/{deviceType}")
    public Result<List<Device>> getDevicesByType(@PathVariable String deviceType) {
        List<Device> devices = deviceService.getDevicesByType(deviceType);
        return Result.success(devices);
    }


    @GetMapping("/{id}")
    public Result<Device> getDevice(@PathVariable Integer id) {
        Device device = deviceService.getDeviceById(id);
        return Result.success(device);
    }


    @PostMapping
    public Result<Device> addDevice(@Valid @RequestBody DeviceDTO deviceDTO) {
        Device device = deviceService.addDevice(deviceDTO);
        return Result.success(device);
    }


    @PutMapping("/{id}")
    public Result<Device> updateDevice(@PathVariable Integer id,
                                       @Valid @RequestBody DeviceDTO deviceDTO) {
        Device device = deviceService.updateDevice(id, deviceDTO);
        return Result.success(device);
    }


    @DeleteMapping("/{id}")
    public Result<Void> deleteDevice(@PathVariable Integer id) {
        deviceService.deleteDevice(id);
        return Result.success();
    }


    @PostMapping("/{id}/control")
    public Result<Void> controlDevice(@PathVariable Integer id,
                                      @RequestParam String command,
                                      @RequestParam(required = false) String param) {
        deviceService.controlDevice(id, command, param);
        return Result.success();
    }
}