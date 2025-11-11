package com.poly.bezbe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor // <-- Bắt buộc cho JPQL
public class TopSellingProductDTO {
    private String productName;
    private long totalSold; // Tổng số lượng đã bán
}