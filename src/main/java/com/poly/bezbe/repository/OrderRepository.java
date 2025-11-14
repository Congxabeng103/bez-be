package com.poly.bezbe.repository;

import com.poly.bezbe.dto.response.CategorySalesDTO;
import com.poly.bezbe.dto.response.MonthlyRevenueDTO;
import com.poly.bezbe.dto.response.TopSellingProductDTO;
import com.poly.bezbe.entity.Order;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // --- THÊM HÀM MỚI NÀY ---
    @Query("SELECT o FROM Order o WHERE " +
            "(:status IS NULL OR o.orderStatus = :status) AND " +
            "(:searchTerm IS NULL OR " +
            " o.orderNumber LIKE %:searchTerm% OR " +
            " o.customerName LIKE %:searchTerm% OR " +
            " o.phone LIKE %:searchTerm%)")
    Page<Order> findByAdminFilters(
            @Param("status") OrderStatus status,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    // --- THÊM HÀM MỚI NÀY CHO USER ---
    /**
     * Tìm tất cả đơn hàng của một User, sắp xếp theo ngày mới nhất
     */
    Page<Order> findByUser(User user, Pageable pageable);

    /**
     * Tìm một đơn hàng cụ thể, đảm bảo nó thuộc về đúng User
     */
    Optional<Order> findByIdAndUser(Long id, User user);


    // --- THÊM 5 HÀM MỚI CHO DASHBOARD ---

    // 1. CHO KPI: Đếm đơn hàng theo trạng thái
    long countByOrderStatus(OrderStatus status);

    // 2. CHO KPI: Tính tổng doanh thu theo trạng thái
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.orderStatus = :status")
    BigDecimal sumTotalAmountByOrderStatus(@Param("status") OrderStatus status);

    // 3. CHO BIỂU ĐỒ DOANH THU THÁNG
    @Query("SELECT new com.poly.bezbe.dto.response.MonthlyRevenueDTO(" +
            "CAST(MONTH(o.createdAt) AS string), SUM(o.totalAmount), COUNT(o)) " +
            "FROM Order o " +
            "WHERE o.orderStatus = :status AND YEAR(o.createdAt) = YEAR(CURRENT_DATE) " +
            "GROUP BY MONTH(o.createdAt) " +
            "ORDER BY MONTH(o.createdAt) ASC")
    List<MonthlyRevenueDTO> findMonthlyRevenue(@Param("status") OrderStatus status);

    // 4. CHO BIỂU ĐỒ DANH MỤC
    @Query("SELECT new com.poly.bezbe.dto.response.CategorySalesDTO(" +
            "c.name, SUM(oi.totalPrice)) " +
            "FROM Order o " +
            "JOIN o.orderItems oi " +
            "JOIN oi.variant v " +
            "JOIN v.product p " +
            "JOIN p.category c " +
            "WHERE o.orderStatus = :status " +
            "GROUP BY c.name " +
            "ORDER BY SUM(oi.totalPrice) DESC")
    List<CategorySalesDTO> findCategorySales(@Param("status") OrderStatus status);

    // 5. CHO TOP 5 SẢN PHẨM
    @Query("SELECT new com.poly.bezbe.dto.response.TopSellingProductDTO(p.name, SUM(oi.quantity)) " +
            "FROM Order o " +
            "JOIN o.orderItems oi " +
            "JOIN oi.variant v " +
            "JOIN v.product p " +
            "WHERE o.orderStatus = :status " +
            "GROUP BY p.name " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<TopSellingProductDTO> findTopSellingProducts(@Param("status") OrderStatus status, Pageable pageable);
    // 1. Hàm cho tab "Chờ hoàn tiền"
    @Query("SELECT o FROM Order o WHERE " +
            "o.paymentStatus = com.poly.bezbe.enums.PaymentStatus.PENDING_REFUND " +
            "AND (:searchTerm IS NULL OR o.orderNumber LIKE %:searchTerm% OR o.customerName LIKE %:searchTerm% OR o.phone LIKE %:searchTerm%)")
    Page<Order> findOrdersPendingRefund(String searchTerm, Pageable pageable);

    // 2. Hàm cho tab "Chờ nhập kho"
    @Query("SELECT o FROM Order o WHERE " +
            "o.orderStatus = com.poly.bezbe.enums.OrderStatus.CANCELLED " +
            "AND o.stockReturned = false " + // <-- Lọc các đơn chưa nhập kho
            "AND (:searchTerm IS NULL OR o.orderNumber LIKE %:searchTerm% OR o.customerName LIKE %:searchTerm% OR o.phone LIKE %:searchTerm%)")
    Page<Order> findOrdersPendingStockReturn(String searchTerm, Pageable pageable);
    Integer countByUserId(Long userId);
}