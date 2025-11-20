package com.poly.bezbe.service;

import com.poly.bezbe.dto.response.*;

import java.util.List;

public interface DashboardService {
    DashboardStatsDTO getDashboardStats();
    List<MonthlyRevenueDTO> getMonthlyRevenue(Integer year, Long productId);
    List<CategorySalesDTO> getCategorySales();
    List<TopSellingProductDTO> getTopSellingProducts();
    List<Integer> getAvailableYears(); // Lấy list năm
    List<ProductSelectDTO> getAllProductsForFilter(); // Lấy list sản phẩm
}