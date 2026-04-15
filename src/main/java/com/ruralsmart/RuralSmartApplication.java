package com.ruralsmart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RuralSmartApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuralSmartApplication.class, args);
    }
}