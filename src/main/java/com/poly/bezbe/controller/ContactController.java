package com.poly.bezbe.controller;

import com.poly.bezbe.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contact") // <-- Giống với đường dẫn frontend gọi
@RequiredArgsConstructor
public class ContactController {

    private final EmailService emailService;

    // DTO (Data Transfer Object) để nhận dữ liệu
    // (Bạn nên tạo file ContactRequestDTO.java riêng)
    static class ContactRequestDTO {
        public String name;
        public String email;
        public String subject;
        public String message;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> handleContactForm(@RequestBody ContactRequestDTO request) {
        try {
            emailService.sendContactFormToAdmin(
                    request.name,
                    request.email,
                    request.subject,
                    request.message
            );
            return ResponseEntity.ok(Map.of("message", "Gửi tin nhắn thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi máy chủ"));
        }
    }
}