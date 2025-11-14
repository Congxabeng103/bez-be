package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // --- CÁC HÀM LỌC THEO TRẠNG THÁI (Giữ lại) ---
    Page<Category> findByNameContainingIgnoreCaseAndActive(String name, boolean active, Pageable pageable);
    Page<Category> findAllByActive(boolean active, Pageable pageable);
    List<Category> findAllByActiveTrue(Sort sort); // (Dùng cho Product form nếu bạn chỉ muốn SP active)

    // --- THÊM 2 HÀM MỚI (Để lấy TẤT CẢ) ---

    // Tìm theo tên (không quan tâm trạng thái)
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Lấy tất cả (đã có sẵn trong JpaRepository, nhưng khai báo lại cho rõ)
    Page<Category> findAll(Pageable pageable);

    // Lấy tất cả (đã có sẵn, dùng cho /all-brief nếu bạn muốn)
    List<Category> findAll(Sort sort);
    boolean existsByNameIgnoreCase(String name);

    // Dùng cho hàm updateCategory
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}