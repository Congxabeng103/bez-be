package com.poly.bezbe.controller;

import com.poly.bezbe.dto.response.OrderResponseDTO;
import com.poly.bezbe.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller // (Dùng @Controller vì chúng ta sẽ redirect)
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;

    // VNPAY SẼ GỌI VÀO ĐƯỜNG DẪN NÀY (VD: /api/v1/payment/vnpay-return?...)
    @GetMapping("/vnpay-return")
    public RedirectView handleVnpayReturn(HttpServletRequest request) {

        OrderResponseDTO orderResponse = orderService.handleVnpayReturn(request);

        // Lấy mã đơn hàng từ response
        String orderId = String.valueOf(orderResponse.getOrderId());

        // Lấy trạng thái thanh toán
        String paymentStatus = orderResponse.getPaymentStatus();

        // Chuyển hướng FE về trang kết quả
        String redirectUrl = "http://localhost:3000/order-confirmation?orderId=" + orderId;
        if (!"PAID".equals(paymentStatus)) {
            redirectUrl += "&status=failed";
        }

        return new RedirectView(redirectUrl);
    }
}