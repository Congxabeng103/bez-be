package com.poly.bezbe;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class BezBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BezBeApplication.class, args);
    }
    // Thiết lập múi giờ mặc định cho toàn bộ ứng dụng khi khởi động
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }
}
