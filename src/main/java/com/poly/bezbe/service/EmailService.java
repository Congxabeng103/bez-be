package com.poly.bezbe.service;

import com.poly.bezbe.entity.Order;
import jakarta.mail.MessagingException;

import java.math.BigDecimal;

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

    // --- THÊM 4 HÀM MỚI ---

    /**
     * Gửi mail Xác nhận Đặt hàng (Thường cho COD, hoặc VNPAY PENDING)
     */
    void sendOrderConfirmationEmail(Order order);

    /**
     * Gửi mail Xác nhận Thanh toán thành công (Cho VNPAY PAID)
     */
    void sendPaymentSuccessEmail(Order order);

    /**
     * Gửi mail Thông báo Giao hàng (Khi Admin chuyển sang SHIPPING)
     */
    void sendShippingNotificationEmail(Order order);

    /**
     * Gửi mail Thông báo Hủy đơn
     */
    void sendOrderCancellationEmail(Order order,String reason);
    void sendDisputeReceivedEmail(Order order, String reason);
    void sendOrderRefundNotificationEmail(Order order, BigDecimal refundAmount);
    void sendOrderDeliveredEmail(Order order);
    void sendContactFormToAdmin(String fromName, String fromEmail, String subject, String message);
}