package com.example.eco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableRetry
@EnableAsync
@EnableAspectJAutoProxy
@SpringBootApplication
public class EcoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcoApplication.class, args);
    }

}
