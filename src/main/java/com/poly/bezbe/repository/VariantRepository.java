package com.poly.bezbe.repository;

import com.poly.bezbe.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VariantRepository extends JpaRepository<ProductVariant, Long> {

    /**
     * Tìm biến thể theo SKU (kiểm tra trùng lặp khi tạo mới)
     */
    Optional<ProductVariant> findBySku(String sku);

    /**
     * Tìm biến thể theo SKU (kiểm tra trùng lặp khi cập nhật, trừ ID hiện tại)
     */
    Optional<ProductVariant> findBySkuAndIdNot(String sku, Long id);

    /**
     * Hàm tìm kiếm chính cho Bảng Quản lý Biến thể.
     * Lọc theo ID sản phẩm, trạng thái (ACTIVE, INACTIVE, ALL) và tìm kiếm (theo SKU).
     */
    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId " +
            "AND (:searchTerm IS NULL OR LOWER(v.sku) LIKE LOWER(concat('%', :searchTerm, '%'))) " + // (Bạn có thể thêm tìm kiếm theo thuộc tính nếu muốn)
            "AND (:status = 'ALL' OR v.active = :active)")
    Page<ProductVariant> findByProductAndSearchAndStatus(
            @Param("productId") Long productId,
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("active") boolean active,
            Pageable pageable
    );
    List<ProductVariant> findByProductId(Long productId);
    // 1. Dùng để đếm số biến thể cho 1 sản phẩm
    long countByProductId(Long productId);

    // 2. Dùng để ẩn hàng loạt biến thể khi ẩn sản phẩm
    List<ProductVariant> findAllByProductIdAndActive(Long productId, boolean active);
}