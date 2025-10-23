package com.poly.bezbe.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PromotionBriefDTO {
    private Long id; // ID khuyến mãi
    private String name; // Tên khuyến mãi (để hiển thị trong dropdown)
}
