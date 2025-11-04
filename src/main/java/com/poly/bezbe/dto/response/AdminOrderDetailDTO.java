package com.poly.bezbe.dto.response;

import com.poly.bezbe.enums.OrderStatus;
import com.poly.bezbe.enums.PaymentMethod;
import com.poly.bezbe.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminOrderDetailDTO {
    // Thông tin cơ bản
    private Long id;
    private String orderNumber;
    private LocalDateTime createdAt;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;

    // Thông tin Khách hàng
    private String customerName;
    private String phone;
    private String email;
    private String address;
    private String note;

    // Thông tin Tiền
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal couponDiscount;
    private BigDecimal totalAmount;

    // Danh sách sản phẩm
    private List<AdminOrderItemDTO> items;
}