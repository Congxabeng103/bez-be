package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Order;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}