package com.poly.bezbe.service.impl; // <-- Chú ý package con 'impl'

import com.poly.bezbe.service.EmailService; // <-- Import interface
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service // <-- @Service được đặt ở đây
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService { // <-- Implement interface

    private final JavaMailSender mailSender;

    /**
     * {@inheritDoc}
     * <p>
     * Thêm @Async để gửi email trong một luồng riêng,
     * giúp API trả về response ngay lập tức mà không cần chờ email gửi xong.
     */
    @Async // <-- @Async được giữ ở lớp Impl
    @Override // <-- Thêm @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        // true = multipart message (cần thiết cho HTML/attachments)
        // "utf-8" = hỗ trợ tiếng Việt
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // true = nội dung là HTML

        // (Bạn có thể cấu hình 'from' trong file application.properties
        // hoặc set cứng ở đây nếu muốn)
        // helper.setFrom("no-reply@bezbe.com", "BezBe Store");

        mailSender.send(mimeMessage);
    }
}