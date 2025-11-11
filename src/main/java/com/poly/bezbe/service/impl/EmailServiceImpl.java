package com.poly.bezbe.service.impl; // <-- Chú ý package con 'impl'

import com.poly.bezbe.entity.Order;
import com.poly.bezbe.service.EmailService; // <-- Import interface
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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

    // --- IMPLEMENT 4 HÀM MỚI ---

    @Override
    public void sendOrderConfirmationEmail(Order order) {
        try {
            String subject = "Xác nhận đơn hàng #" + order.getOrderNumber();
            String htmlBody = String.format(
                    "<h1>Cảm ơn bạn đã đặt hàng!</h1>" +
                            "<p>Chào %s,</p>" +
                            "<p>Chúng tôi đã nhận được đơn hàng #%s của bạn (Tổng tiền: %s VND).</p>" +
                            "<p>Chúng tôi sẽ xử lý và liên hệ với bạn sớm nhất.</p>",
                    order.getCustomerName(),
                    order.getOrderNumber(),
                    order.getTotalAmount().toString()
            );
            sendHtmlEmail(order.getEmail(), subject, htmlBody);
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi mail xác nhận đơn hàng: " + e.getMessage());
        }
    }

    @Override
    public void sendPaymentSuccessEmail(Order order) {
        try {
            String subject = "Thanh toán thành công đơn hàng #" + order.getOrderNumber();
            String htmlBody = String.format(
                    "<h1>Thanh toán thành công!</h1>" +
                            "<p>Chào %s,</p>" +
                            "<p>Chúng tôi xác nhận đã nhận thanh toán thành công cho đơn hàng #%s.</p>" +
                            "<p>Đơn hàng của bạn đang được xử lý.</p>",
                    order.getCustomerName(),
                    order.getOrderNumber()
            );
            sendHtmlEmail(order.getEmail(), subject, htmlBody);
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi mail thanh toán thành công: " + e.getMessage());
        }
    }

    @Override
    public void sendShippingNotificationEmail(Order order) {
        try {
            String subject = "Đơn hàng #" + order.getOrderNumber() + " đang được giao";
            String htmlBody = String.format(
                    "<h1>Đơn hàng đang trên đường!</h1>" +
                            "<p>Chào %s,</p>" +
                            "<p>Đơn hàng #%s của bạn đã được bàn giao cho đơn vị vận chuyển.</p>" +
                            "<p>Vui lòng chú ý điện thoại để nhận hàng.</p>",
                    order.getCustomerName(),
                    order.getOrderNumber()
            );
            sendHtmlEmail(order.getEmail(), subject, htmlBody);
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi mail thông báo giao hàng: " + e.getMessage());
        }
    }

    @Override
    public void sendOrderCancellationEmail(Order order,String reason) {
        try {
            String subject = "Đã hủy đơn hàng #" + order.getOrderNumber();
            String htmlBody = String.format(
                    "<h1>Đơn hàng đã bị hủy</h1>" +
                            "<p>Chào %s,</p>" +
                            "<p>Đơn hàng #%s của bạn đã được hủy theo yêu cầu.</p>" +
                            "<p><b>Lý do:</b> %s</p>" + // <-- THÊM DÒNG NÀY
                            "<p>Nếu bạn không thực hiện hành động này, vui lòng liên hệ chúng tôi.</p>",
                    order.getCustomerName(),
                    order.getOrderNumber(),
                    reason
            );
            sendHtmlEmail(order.getEmail(), subject, htmlBody);
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi mail hủy đơn: " + e.getMessage());
        }
    }
    @Override
    public void sendOrderRefundNotificationEmail(Order order, BigDecimal refundAmount) {
        try {
            String subject = "Thông báo hoàn tiền đơn hàng #" + order.getOrderNumber();
            String htmlBody = String.format(
                    "<h1>Đã hoàn tiền thành công</h1>" +
                            "<p>Chào %s,</p>" +
                            "<p>Chúng tôi đã xử lý thành công yêu cầu hoàn tiền cho đơn hàng #%s.</p>" +
                            "<p>Số tiền: <b>%s VND</b> đã được hoàn về tài khoản/thẻ VNPAY của bạn.</p>" +
                            "<p>(Thời gian tiền về tài khoản tùy thuộc vào ngân hàng của bạn, thường là 1-5 ngày làm việc).</p>",
                    order.getCustomerName(),
                    order.getOrderNumber(),
                    refundAmount.toBigInteger().toString() // Hiển thị số tiền
            );
            // Gửi bất đồng bộ
            sendHtmlEmail(order.getEmail(), subject, htmlBody);
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi mail hoàn tiền: " + e.getMessage());
        }
    }

    @Override
    public void sendOrderDeliveredEmail(Order order) {
        try {
            String subject = "Đơn hàng #" + order.getOrderNumber() + " đã được giao";
            String htmlBody = String.format(
                    "<h1>Đã giao hàng thành công!</h1>" +
                            "<p>Chào %s,</p>" +
                            "<p>Đơn hàng #%s của bạn đã được giao thành công đến địa chỉ của bạn.</p>" +
                            "<p>Vui lòng kiểm tra đơn hàng và bấm 'Đã nhận được hàng' trong mục Đơn hàng của tôi. Cảm ơn bạn!</p>",
                    order.getCustomerName(),
                    order.getOrderNumber()
            );
            sendHtmlEmail(order.getEmail(), subject, htmlBody);
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi mail đã giao hàng: " + e.getMessage());
        }

    }
    @Override
    public void sendDisputeReceivedEmail(Order order, String reason) {
        try {
            String subject = "Đã nhận khiếu nại cho đơn hàng #" + order.getOrderNumber();
            String htmlBody = String.format(
                    "<h1>Chúng tôi đã nhận khiếu nại</h1>" +
                            "<p>Chào %s,</p>" +
                            "<p>Chúng tôi đã nhận được khiếu nại cho đơn hàng #%s.</p>" +
                            "<p><b>Nội dung khiếu nại:</b> %s</p>" + // <-- Thêm lý do
                            "<p>Bộ phận Chăm sóc khách hàng sẽ liên hệ với bạn sớm nhất.</p>",
                    order.getCustomerName(),
                    order.getOrderNumber(),
                    reason
            );
            sendHtmlEmail(order.getEmail(), subject, htmlBody);
            // (Tùy chọn: Gửi mail thông báo cho Admin)
            // sendHtmlEmail("admin@bezbe.com", "CÓ KHIẾU NẠI MỚI", htmlBody);
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi mail khiếu nại: " + e.getMessage());
        }
    }
}
