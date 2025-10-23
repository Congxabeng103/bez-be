package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort; // Import Sort
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; // Import List

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    // Tìm kiếm theo tên (cho bảng danh sách KM)
    Page<Promotion> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Lấy tất cả có phân trang (cho bảng danh sách KM)
    Page<Promotion> findAll(Pageable pageable);

    // Lấy tất cả có sắp xếp (cho dropdown /brief)
    List<Promotion> findAll(Sort sort);

    // Không cần findActiveConflict nữa
}