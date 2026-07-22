package com.activitycube;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.activitycube.mapper")
public class ActivityCubeApplication {
    public static void main(String[] args) {
        SpringApplication.run(ActivityCubeApplication.class, args);
    }
}
