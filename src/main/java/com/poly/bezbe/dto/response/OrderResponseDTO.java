package com.poly.bezbe.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponseDTO {
    private Long orderId;
    private String orderStatus;
    private String paymentStatus;
    private String paymentMethod;
    private String customerName;
    private String phone;
    private String address;
    private String note; // Ghi chú của khách
    private String cancellationReason; // Lý do hủy
    private String disputeReason; // Lý do khiếu nại
}