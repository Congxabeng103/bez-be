package com.poly.bezbe.dto.response;

import com.poly.bezbe.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundResponseDTO {
    private Long orderId;
    private PaymentStatus newPaymentStatus; // Trạng thái mới (REFUNDED)
    private String message; // Thông báo (VD: "Hoàn tiền thành công")
    private String vnpayResponseCode; // Mã lỗi VNPAY (VD: "00")
}