package com.example.aimanager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@MapperScan("com.example.aimanager.mapper")
public class AiManagerApplication {
    public static void main(String[] args) {
        try {
            ConfigurableApplicationContext ctx = SpringApplication.run(AiManagerApplication.class, args);
            System.out.println("=== APPLICATION STARTED SUCCESSFULLY: " + ctx.getId() + " ===");
        } catch (Throwable t) {
            System.err.println("=== APPLICATION FAILED TO START ===");
            System.err.println("Exception: " + t.getClass().getName());
            System.err.println("Message: " + t.getMessage());
            t.printStackTrace(System.err);
            System.err.flush();
            System.exit(1);
        }
    }
}
