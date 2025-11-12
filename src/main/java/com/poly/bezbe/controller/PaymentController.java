package com.poly.bezbe.controller;

import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.OrderResponseDTO;
import com.poly.bezbe.dto.response.RefundResponseDTO;
import com.poly.bezbe.dto.response.VnpayResponseDTO;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // <-- 1. IMPORT
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@Controller // (Dùng @Controller vì chúng ta sẽ redirect)
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;

    // VNPAY SẼ GỌI VÀO ĐƯỜNG DẪN NÀY (PUBLIC)
    @GetMapping("/vnpay-return")
    public RedirectView handleVnpayReturn(HttpServletRequest request) {

        // 1. Hàm này giờ chỉ lấy thông tin "hiện tại"
        OrderResponseDTO orderResponse = orderService.handleVnpayReturn(request);

        String orderId = String.valueOf(orderResponse.getOrderId());

        // 2. Lấy trạng thái thanh toán HIỆN TẠI từ DB
        String paymentStatus = orderResponse.getPaymentStatus();

        // 3. SỬA LOGIC REDIRECT
        String redirectUrl = "http://localhost:3000/order-confirmation?orderId=" + orderId;

        // Lấy "gợi ý" từ VNPAY
        String vnpayStatus = request.getParameter("vnp_ResponseCode");
        if (vnpayStatus != null && !vnpayStatus.equals("00")) {
            // Nếu VNPAY gợi ý là lỗi, thêm status=failed ngay
            redirectUrl += "&status=failed";
        }

        return new RedirectView(redirectUrl);
    }

    /**
     * API cho User bấm nút "Thanh toán lại" (cho đơn VNPAY PENDING)
     */
    @PostMapping("/{orderId}/retry-vnpay")
    @PreAuthorize("isAuthenticated()") // <-- 2. THÊM (Chỉ user đăng nhập)
    @ResponseBody
    public ResponseEntity<ApiResponseDTO<VnpayResponseDTO>> retryVnpayPayment(
            @AuthenticationPrincipal User user, // Lấy user để bảo mật
            @PathVariable Long orderId,
            HttpServletRequest httpServletRequest // Cần để lấy IP mới
    ) {
        // Hàm này sẽ tìm đơn hàng, kiểm tra, và tạo link VNPAY mới
        VnpayResponseDTO vnpayResponse = orderService.retryVnpayPayment(user, orderId, httpServletRequest);

        return ResponseEntity.ok(ApiResponseDTO.success(vnpayResponse, "Tạo link thanh toán lại thành công"));
    }

    /**
     * Luồng 2: VNPAY TỰ GỌI VỀ (Server-to-Server) (PUBLIC)
     */
    @GetMapping("/vnpay-ipn")
    @ResponseBody
    public ResponseEntity<String> handleVnpayIpn(HttpServletRequest request) {

        // Gọi sang OrderService để xử lý logic IPN
        String vnpayResponse = orderService.handleVnpayIpn(request);

        // Trả về response cho VNPAY (Rất quan trọng)
        return ResponseEntity.ok(vnpayResponse);
    }

    // NGHIỆP VỤ TIỀN (Hoàn tiền VNPAY)
    @PostMapping("/refund/vnpay/{orderId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')") // <-- 3. KHÓA (STAFF không được)
    @ResponseBody
    public ResponseEntity<ApiResponseDTO<RefundResponseDTO>> requestVnpayRefund(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User currentUser, // Lấy admin/staff đang đăng nhập
            HttpServletRequest request // Cần để lấy IP
    ) {
        // 1. Kiểm tra bảo mật
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponseDTO.error("Bạn cần đăng nhập để thực hiện hành động này."));
        }

        // 2. Gọi service
        RefundResponseDTO refundResponse = orderService.requestVnpayRefund(
                orderId,
                request,
                currentUser
        );

        // 3. Trả về thành công
        return ResponseEntity.ok(ApiResponseDTO.success(
                refundResponse,
                refundResponse.getMessage() // Lấy thông báo "Hoàn tiền thành công!"
        ));
    }
}