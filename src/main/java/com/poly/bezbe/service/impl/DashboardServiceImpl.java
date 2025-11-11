package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.response.*;
import com.poly.bezbe.enums.OrderStatus;
import com.poly.bezbe.enums.Role; // <-- Import Role
import com.poly.bezbe.repository.OrderRepository;
import com.poly.bezbe.repository.ProductRepository;
import com.poly.bezbe.repository.UserRepository;
import com.poly.bezbe.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // Đặt trạng thái thành công ở đây để dễ dàng thay đổi (VD: COMPLETED hoặc DELIVERED)
    private final OrderStatus SUCCESS_STATUS = OrderStatus.COMPLETED;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        // 1. Doanh thu
        BigDecimal totalRevenue = orderRepository.sumTotalAmountByOrderStatus(SUCCESS_STATUS);

        // 2. Đơn hàng
        long totalCompletedOrders = orderRepository.countByOrderStatus(SUCCESS_STATUS);

        // 3. Khách hàng (Đã sửa lại)
        long totalCustomers = userRepository.countByRole(Role.USER);

        // 4. Sản phẩm
        long totalProducts = productRepository.count();

        return DashboardStatsDTO.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalCompletedOrders(totalCompletedOrders)
                .totalCustomers(totalCustomers)
                .totalProducts(totalProducts)
                .build();
    }

    @Override
    public List<MonthlyRevenueDTO> getMonthlyRevenue() {
        // Lấy data thô (chỉ các tháng có doanh thu)
        List<MonthlyRevenueDTO> revenueData = orderRepository.findMonthlyRevenue(SUCCESS_STATUS);

        // Chuyển List sang Map (Key: "1", "2"...)
        Map<String, MonthlyRevenueDTO> revenueMap = revenueData.stream()
                .collect(Collectors.toMap(MonthlyRevenueDTO::getMonth, dto -> dto));

        // Mảng tên 12 tháng
        String[] monthNames = {
                "Thg 1", "Thg 2", "Thg 3", "Thg 4", "Thg 5", "Thg 6",
                "Thg 7", "Thg 8", "Thg 9", "Thg 10", "Thg 11", "Thg 12"
        };

        // Tạo list 12 tháng "chuẩn" (lấp đầy các tháng 0 doanh thu)
        return IntStream.range(0, 12)
                .mapToObj(i -> {
                    String monthKey = String.valueOf(i + 1); // "1", "2"...
                    String monthName = monthNames[i]; // "Thg 1", "Thg 2"...

                    if (revenueMap.containsKey(monthKey)) {
                        MonthlyRevenueDTO dto = revenueMap.get(monthKey);
                        dto.setMonth(monthName); // Đổi "1" -> "Thg 1"
                        return dto;
                    } else {
                        return new MonthlyRevenueDTO(monthName, BigDecimal.ZERO, 0L);
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CategorySalesDTO> getCategorySales() {
        // 1. Lấy TẤT CẢ danh mục (Repo đã tự sắp xếp DESC)
        List<CategorySalesDTO> allCategories = orderRepository.findCategorySales(SUCCESS_STATUS);

        // 2. Đặt giới hạn: Chỉ hiển thị 4 danh mục lớn nhất + 1 "Khác"
        // (Bạn có thể đổi số 4 này thành 5 hoặc 6 nếu muốn)
        int maxTopCategories = 4;

        // 3. Nếu tổng số danh mục <= 5 (ví dụ 4, 5), không cần gộp, trả về luôn
        if (allCategories.size() <= maxTopCategories + 1) {
            return allCategories;
        }

        // 4. Nếu có nhiều (ví dụ 20 danh mục)
        // Lấy Top 4
        List<CategorySalesDTO> topCategories = allCategories.subList(0, maxTopCategories);

        // 5. Tính tổng "Khác" (tất cả các danh mục từ index 4 trở đi)
        BigDecimal otherValue = allCategories.subList(maxTopCategories, allCategories.size())
                .stream()
                .map(CategorySalesDTO::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add); // Cộng tất cả lại

        // 6. Tạo list kết quả
        // (Phải tạo ArrayList mới vì subList() không thể add)
        List<CategorySalesDTO> result = new ArrayList<>(topCategories); // Gồm 4 mục Top
        result.add(new CategorySalesDTO("Khác", otherValue)); // Thêm mục "Khác"

        return result; // Trả về danh sách 5 mục
    }


    @Override
    public List<TopSellingProductDTO> getTopSellingProducts() {
        // Chỉ lấy Top 5
        Pageable topFive = PageRequest.of(0, 5);
        return orderRepository.findTopSellingProducts(SUCCESS_STATUS, topFive);
    }
}