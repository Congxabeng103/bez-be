package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph; // Import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import
import org.springframework.data.repository.query.Param; // Import
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1. Dùng cho getAllProducts (Bảng Quản lý Sản phẩm, lọc theo Status)
    @EntityGraph(attributePaths = {"promotion", "category", "brand"}) // Tải EAGER các liên kết
    @Query("SELECT p FROM Product p WHERE " +
            "(:searchTerm IS NULL OR LOWER(p.name) LIKE LOWER(concat('%', :searchTerm, '%'))) " +
            "AND (:status = 'ALL' OR p.active = :active)")
    Page<Product> findBySearchAndStatus(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("active") boolean active,
            Pageable pageable
    );

    // 2. Dùng cho getProductBriefList (Chỉ lấy Active, TÌM KIẾM)
    @EntityGraph(attributePaths = {"variants"})
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    // 3. Dùng cho getProductBriefList (Chỉ lấy Active, KHÔNG TÌM KIẾM)
    @EntityGraph(attributePaths = {"variants"})
    Page<Product> findAllByActiveTrue(Pageable pageable);

    // THÊM HÀM NÀY (Để đếm sản phẩm)
    long countByCategoryId(Long categoryId);

    // THÊM HÀM NÀY (Để ẩn hàng loạt)
    List<Product> findAllByCategoryIdAndActive(Long categoryId, boolean active);

    // (Lát nữa bạn cũng sẽ cần 2 hàm tương tự cho Brand)
     long countByBrandId(Long brandId);
     List<Product> findAllByBrandIdAndActive(Long brandId, boolean active);
}