package com.poly.bezbe.controller;

import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.OrderResponseDTO;
import com.poly.bezbe.dto.response.VnpayResponseDTO;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
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

    // --- THÊM HÀM MỚI NÀY ---

    /**
     * API cho User bấm nút "Thanh toán lại" (cho đơn VNPAY PENDING)
     */
    @PostMapping("/{orderId}/retry-vnpay")
    @ResponseBody // (Cần thiết vì class là @Controller, nhưng hàm này trả về JSON)
    public ResponseEntity<ApiResponseDTO<VnpayResponseDTO>> retryVnpayPayment(
            @AuthenticationPrincipal User user, // Lấy user để bảo mật
            @PathVariable Long orderId,
            HttpServletRequest httpServletRequest // Cần để lấy IP mới
    ) {
        // Hàm này sẽ tìm đơn hàng, kiểm tra, và tạo link VNPAY mới
        VnpayResponseDTO vnpayResponse = orderService.retryVnpayPayment(user, orderId, httpServletRequest);

        return ResponseEntity.ok(ApiResponseDTO.success(vnpayResponse, "Tạo link thanh toán lại thành công"));
    }
}