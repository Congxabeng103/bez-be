package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    // 1. Kiểm tra trùng lặp tên
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // 2. Lấy danh sách active (cho dropdown /brief)
    List<Promotion> findAllByActiveTrue(Sort sort);

    // 3. Hàm tìm kiếm chính (hỗ trợ status, search, phân trang)
    @Query("SELECT p FROM Promotion p WHERE " +
            "(:searchTerm IS NULL OR LOWER(p.name) LIKE LOWER(concat('%', :searchTerm, '%'))) " +
            "AND (:status = 'ALL' OR p.active = :active)")
    Page<Promotion> findBySearchAndStatus(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("active") boolean active,
            Pageable pageable
    );
}