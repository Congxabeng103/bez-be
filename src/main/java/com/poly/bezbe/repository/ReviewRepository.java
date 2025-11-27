package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Review;
import com.poly.bezbe.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // =================================================================
    // NHÓM 1: DÀNH CHO ADMIN/MANAGER (Lấy tất cả, kể cả ẩn)
    // =================================================================

    // 1. Lấy tất cả (Mặc định)
    Page<Review> findByProductId(Long productId, Pageable pageable);

    // 2. Lọc theo sao (Lấy tất cả)
    Page<Review> findByProductIdAndRating(Long productId, Integer rating, Pageable pageable);


    // =================================================================
    // NHÓM 2: DÀNH CHO KHÁCH HÀNG (Chỉ lấy cái hiện - visible = true)
    // =================================================================

    // 3. Lấy danh sách hiển thị
    // SỬA: Đổi từ IsVisibleTrue -> VisibleTrue (để khớp với tên biến 'visible' trong Entity)
    Page<Review> findByProductIdAndVisibleTrue(Long productId, Pageable pageable);

    // 4. Lọc theo sao hiển thị
    // SỬA: Đổi từ IsVisibleTrue -> VisibleTrue
    Page<Review> findByProductIdAndRatingAndVisibleTrue(Long productId, Integer rating, Pageable pageable);


    // =================================================================
    // NHÓM 3: THỐNG KÊ & CHECK LOGIC
    // =================================================================

    // 5. Đếm số lượng từng loại sao (Chỉ đếm cái đang hiện)
    // SỬA: Trong câu Query, đổi r.isVisible -> r.visible
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.visible = true GROUP BY r.rating")
    List<Object[]> countReviewsByRating(@Param("productId") Long productId);

    // 6. Check đã review chưa
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // 7. Check đã mua hàng thành công chưa
    @Query("SELECT COUNT(o) > 0 FROM Order o " +
            "JOIN o.orderItems oi " +
            "JOIN oi.variant v " +
            "WHERE o.user.id = :userId " +
            "AND v.product.id = :productId " +
            "AND o.orderStatus = :status")
    boolean existsByUserAndProductAndStatus(@Param("userId") Long userId,
                                            @Param("productId") Long productId,
                                            @Param("status") OrderStatus status);
}