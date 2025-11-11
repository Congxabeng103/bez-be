package com.poly.bezbe.service;

import com.poly.bezbe.dto.response.CategorySalesDTO;
import com.poly.bezbe.dto.response.DashboardStatsDTO;
import com.poly.bezbe.dto.response.MonthlyRevenueDTO;
import com.poly.bezbe.dto.response.TopSellingProductDTO;
import java.util.List;

public interface DashboardService {
    DashboardStatsDTO getDashboardStats();
    List<MonthlyRevenueDTO> getMonthlyRevenue();
    List<CategorySalesDTO> getCategorySales();
    List<TopSellingProductDTO> getTopSellingProducts();
}