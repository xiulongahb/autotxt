package com.autotxt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutoTxtApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoTxtApplication.class, args);
    }
}
