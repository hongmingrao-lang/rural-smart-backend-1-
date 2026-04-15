package com.ruralsmart.config;

import com.ruralsmart.entity.Device;
import com.ruralsmart.entity.User;
import com.ruralsmart.repository.DeviceRepository;
import com.ruralsmart.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Value("${mqtt.topics.device-control:ruralsmart003}")
    private String controlTopic;

    @Override
    public void run(String... args) {
        initUsers();
        initDevices();
    }

    private void initUsers() {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setRole("admin");
            userRepository.save(admin);

            User user = new User();
            user.setUsername("user");
            user.setPassword("user123");
            user.setRole("user");
            userRepository.save(user);

            log.info("已初始化演示账号: admin/admin123, user/user123");
        }
    }

    private void initDevices() {
        if (deviceRepository.count() == 0) {
            // 传感器设备
            deviceRepository.save(createDevice("温度传感器-A1", "sensor", "temperature", "garden", controlTopic));
            deviceRepository.save(createDevice("湿度传感器-A2", "sensor", "humidity", "garden", controlTopic));
            deviceRepository.save(createDevice("土壤湿度传感器-B1", "sensor", "soil", "garden", controlTopic));

            // 执行器设备
            deviceRepository.save(createDevice("智能空调", "actuator", null, "living_room", controlTopic));
            deviceRepository.save(createDevice("菜地水泵", "actuator", null, "garden", controlTopic));
            deviceRepository.save(createDevice("花园水泵", "actuator", null, "garden", controlTopic));
            deviceRepository.save(createDevice("果树水泵", "actuator", null, "garden", controlTopic));
            deviceRepository.save(createDevice("客厅主灯", "actuator", null, "living_room", controlTopic));
            deviceRepository.save(createDevice("卧室灯", "actuator", null, "bedroom", controlTopic));
            deviceRepository.save(createDevice("厨房灯", "actuator", null, "kitchen", controlTopic));
            deviceRepository.save(createDevice("卫生间灯", "actuator", null, "bathroom", controlTopic));
            deviceRepository.save(createDevice("走廊灯", "actuator", null, "hallway", controlTopic));
            deviceRepository.save(createDevice("车库灯", "actuator", null, "garage", controlTopic));

            log.info("已初始化默认设备数据（传感器3个 + 执行器10个）");
        }
    }

    private Device createDevice(String name, String deviceType, String sensorType, String location, String mqttTopic) {
        Device device = new Device();
        device.setName(name);
        device.setDeviceType(deviceType);
        device.setSensorType(sensorType);
        device.setLocation(location);
        device.setMqttTopic(mqttTopic);
        device.setStatus(true);
        device.setOnline(false);
        return device;
    }
}
