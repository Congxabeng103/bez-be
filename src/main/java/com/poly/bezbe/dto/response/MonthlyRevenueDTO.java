package com.poly.bezbe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor // <-- Bắt buộc cho JPQL
public class MonthlyRevenueDTO {
    private String month;
    private BigDecimal revenue;
    private long orders;
}