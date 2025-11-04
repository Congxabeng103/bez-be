package com.poly.bezbe.dto.response;

import com.poly.bezbe.enums.OrderStatus;
import com.poly.bezbe.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminOrderDTO {
    private Long id;
    private String orderNumber; // (Chúng ta sẽ thêm trường này vào Order.java)
    private String customerName;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
}