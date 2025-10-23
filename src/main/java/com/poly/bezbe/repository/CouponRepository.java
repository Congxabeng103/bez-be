package com.poly.bezbe.repository;

// File: repository/CouponRepository.java

import com.poly.bezbe.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    // Tìm kiếm theo code hoặc description
    Page<Coupon> findByCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String code, String description, Pageable pageable);

    Page<Coupon> findAll(Pageable pageable);
    boolean existsByCode(String code); // Kiểm tra trùng code khi tạo
    boolean existsByCodeAndIdNot(String code, Long id); // Kiểm tra trùng code khi sửa
}

