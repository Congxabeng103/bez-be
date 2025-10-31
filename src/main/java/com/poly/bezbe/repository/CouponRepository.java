package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
