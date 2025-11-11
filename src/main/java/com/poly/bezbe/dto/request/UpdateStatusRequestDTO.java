package com.poly.bezbe.dto.request;

import com.poly.bezbe.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequestDTO {
    @NotNull(message = "Trạng thái mới không được để trống")
    private OrderStatus newStatus;

    private String trackingCode; // (Mã vận đơn, nếu có)
    private String note;
}