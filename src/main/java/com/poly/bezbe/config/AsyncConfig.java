package com.poly.bezbe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync // <-- Dòng này "kích hoạt" tất cả các @Async trong dự án
public class AsyncConfig {
    // Không cần nội dung gì bên trong
}