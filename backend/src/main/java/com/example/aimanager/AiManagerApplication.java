package com.example.aimanager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.aimanager.mapper")
public class AiManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiManagerApplication.class, args);
    }
}
