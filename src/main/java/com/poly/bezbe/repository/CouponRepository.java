package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    // 1. Kiểm tra mã trùng (không phân biệt hoa thường)
    boolean existsByCodeIgnoreCase(String code);

    // 2. Kiểm tra mã trùng khi cập nhật (cho ID khác)
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    // 3. Hàm tìm kiếm chính (hỗ trợ status, search, phân trang)
    @Query("SELECT c FROM Coupon c WHERE " +
            "(:searchTerm IS NULL OR LOWER(c.code) LIKE LOWER(concat('%', :searchTerm, '%')) OR LOWER(c.description) LIKE LOWER(concat('%', :searchTerm, '%'))) " +
            "AND (:status = 'ALL' OR c.active = :active)")
    Page<Coupon> findBySearchAndStatus(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("active") boolean active,
            Pageable pageable
    );
    // --- 2. THÊM DÒNG NÀY ĐỂ SỬA LỖI ---
    /**
     * Tìm coupon bằng mã, không phân biệt hoa thường
     */
    Optional<Coupon> findByCodeIgnoreCase(String code);

    // 1. (MỚI) Dùng cho Service (ĐƠN GIẢN)
    /** Tìm tất cả coupon đang active (dùng cho trang chủ) */
    List<Coupon> findByActive(boolean active, Pageable pageable);

    // 2. (MỚI) Dùng cho Scheduler (TỰ ĐỘNG BẬT)
    /** Tìm Coupon sắp bắt đầu (chưa active và ngày bắt đầu là hôm nay) */
    List<Coupon> findAllByActiveAndStartDate(boolean active, LocalDate today);

    // 3. (MỚI) Dùng cho Scheduler (TỰ ĐỘNG TẮT)
    /** Tìm Coupon đã hết hạn (đang active và ngày kết thúc < hôm nay) */
    List<Coupon> findAllByActiveAndEndDateLessThan(boolean active, LocalDate today);
    List<Coupon> findAllByActive(boolean active);
}
