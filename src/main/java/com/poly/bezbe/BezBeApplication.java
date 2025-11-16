package com.poly.bezbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BezBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BezBeApplication.class, args);
    }

}
