package com.poly.bezbe.dto.response;

import com.poly.bezbe.entity.OrderAuditLog;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder // Dùng Builder để convert cho dễ
public class OrderAuditLogResponseDTO {

    // Các trường FE cần
    private Long id;
    private String staffName;
    private String staffEmail; // <--- THÊM DÒNG NÀY
    private String description;
    private String fieldChanged;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;


}