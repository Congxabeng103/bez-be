package com.poly.bezbe.dto.request;

import com.poly.bezbe.enums.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequestDTO {
    // Thông tin người nhận (từ form)
    @NotEmpty(message = "Họ tên không được để trống")
    private String customerName;

    @NotEmpty(message = "Số điện thoại không được để trống")
    private String phone;

    @NotEmpty(message = "Địa chỉ không được để trống")
    private String address;

    private String email;
    private String note;

    // Thông tin thanh toán
    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod; // (COD hoặc VNPAY)

    private String couponCode; // (Mã giảm giá, ví dụ: "SALE100K")
}