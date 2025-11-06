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

        // 1. Hàm này giờ chỉ lấy thông tin "hiện tại"
        OrderResponseDTO orderResponse = orderService.handleVnpayReturn(request);

        String orderId = String.valueOf(orderResponse.getOrderId());

        // 2. Lấy trạng thái thanh toán HIỆN TẠI từ DB
        // (Có thể nó vẫn là PENDING, và đó là điều OK)
        String paymentStatus = orderResponse.getPaymentStatus();

        // 3. SỬA LOGIC REDIRECT
        String redirectUrl = "http://localhost:3000/order-confirmation?orderId=" + orderId;

        // Lấy "gợi ý" từ VNPAY
        String vnpayStatus = request.getParameter("vnp_ResponseCode");
        if (vnpayStatus != null && !vnpayStatus.equals("00")) {
            // Nếu VNPAY gợi ý là lỗi, thêm status=failed ngay
            redirectUrl += "&status=failed";
        }

        // Nếu VNPAY không gợi ý lỗi (vnpayStatus == "00" hoặc null),
        // chúng ta KHÔNG thêm gì cả.
        // Chúng ta để trang FE (với logic Polling) tự quyết định.

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
    // --- THÊM API IPN MỚI ---
    /**
     * Luồng 2: VNPAY TỰ GỌI VỀ (Server-to-Server)
     * Đây là API quan trọng nhất để xác nhận thanh toán
     */
    @GetMapping("/vnpay-ipn")
    @ResponseBody // (Vì class là @Controller nhưng hàm này trả về String/JSON)
    public ResponseEntity<String> handleVnpayIpn(HttpServletRequest request) {

        // Gọi sang OrderService để xử lý logic IPN
        // Hàm này sẽ:
        // 1. Xác thực chữ ký
        // 2. Kiểm tra mã giao dịch
        // 3. Cập nhật trạng thái đơn hàng (PAID/FAILED)
        // 4. Trả về mã cho VNPAY
        String vnpayResponse = orderService.handleVnpayIpn(request);

        // Trả về response cho VNPAY (Rất quan trọng)
        return ResponseEntity.ok(vnpayResponse);
    }
    // --- KẾT THÚC API IPN ---
}