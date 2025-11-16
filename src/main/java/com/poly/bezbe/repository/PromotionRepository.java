package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    // 2. (MỚI) Dùng cho Service (ĐƠN GIẢN)
    /** Tìm 1 KM đang active, ưu tiên cái có ngày kết thúc xa nhất */
    Optional<Promotion> findFirstByActiveOrderByEndDateDesc(boolean active);

    // 3. (MỚI) Dùng cho Scheduler (TỰ ĐỘNG BẬT)
    /** Tìm KM sắp bắt đầu (chưa active và ngày bắt đầu là hôm nay) */
    List<Promotion> findAllByActiveAndStartDate(boolean active, LocalDate today);

    // 4. (MỚI) Dùng cho Scheduler (TỰ ĐỘNG TẮT)
    /** Tìm KM đã hết hạn (đang active và ngày kết thúc < hôm nay) */
    List<Promotion> findAllByActiveAndEndDateLessThan(boolean active, LocalDate today);
    List<Promotion> findAllByActive(boolean active);
}