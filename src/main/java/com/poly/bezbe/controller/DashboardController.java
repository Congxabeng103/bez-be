package com.poly.bezbe.controller;

import com.poly.bezbe.dto.response.*;
import com.poly.bezbe.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // <-- IMPORT
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')") // <-- KHÓA TOÀN BỘ
public class DashboardController {

    private final DashboardService dashboardService;

    // API 1: Cho 4 Thẻ KPI
    @GetMapping("/stats")
    public ResponseEntity<ApiResponseDTO<DashboardStatsDTO>> getStats() {
        DashboardStatsDTO stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponseDTO.success(stats, "Lấy thống kê thành công"));
    }

    // API 2: Cho Biểu đồ Doanh thu tháng
    @GetMapping("/monthly-revenue")
    public ResponseEntity<ApiResponseDTO<List<MonthlyRevenueDTO>>> getMonthlyRevenue() {
        List<MonthlyRevenueDTO> data = dashboardService.getMonthlyRevenue();
        return ResponseEntity.ok(ApiResponseDTO.success(data, "Lấy doanh thu tháng thành công"));
    }

    // API 3: Cho Biểu đồ Danh mục
    @GetMapping("/category-sales")
    public ResponseEntity<ApiResponseDTO<List<CategorySalesDTO>>> getCategorySales() {
        List<CategorySalesDTO> data = dashboardService.getCategorySales();
        return ResponseEntity.ok(ApiResponseDTO.success(data, "Lấy doanh thu danh mục thành công"));
    }

    // API 4: Cho Top 5 Sản phẩm
    @GetMapping("/top-products")
    public ResponseEntity<ApiResponseDTO<List<TopSellingProductDTO>>> getTopProducts() {
        List<TopSellingProductDTO> data = dashboardService.getTopSellingProducts();
        return ResponseEntity.ok(ApiResponseDTO.success(data, "Lấy top sản phẩm thành công"));
    }
}