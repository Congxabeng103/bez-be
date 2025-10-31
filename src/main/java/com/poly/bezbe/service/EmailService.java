package com.poly.bezbe.service;

import jakarta.mail.MessagingException;

/**
 * Interface định nghĩa dịch vụ gửi email.
 */
public interface EmailService {

    /**
     * Gửi một email có nội dung dạng HTML.
     * Việc triển khai (implementation) của hàm này nên là bất đồng bộ (@Async)
     * để không block luồng chính của ứng dụng.
     *
     * @param to       Email người nhận.
     * @param subject  Tiêu đề email.
     * @param htmlBody Nội dung email (dạng HTML).
     * @throws MessagingException Nếu có lỗi trong quá trình tạo hoặc gửi email.
     */
    void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException;
}