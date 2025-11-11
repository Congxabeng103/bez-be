package com.poly.bezbe.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DashboardStatsDTO {
    private BigDecimal totalRevenue;
    private long totalCompletedOrders;
    private long totalCustomers;
    private long totalProducts;
}