package com.poly.bezbe.repository;

import com.poly.bezbe.entity.ProductVariant;
import org.springframework.data.domain.Page; // Thêm Page
import org.springframework.data.domain.Pageable; // Thêm Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Thêm Query
import org.springframework.data.repository.query.Param; // Thêm Param
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // Lấy danh sách biến thể theo ID sản phẩm (cho API GET)
    Page<ProductVariant> findByProduct_Id(Long productId, Pageable pageable);

    // Tìm kiếm biến thể theo SKU hoặc tên thuộc tính/giá trị (hỗ trợ phân trang)
    @Query("SELECT pv FROM ProductVariant pv JOIN pv.attributeValues vv JOIN vv.attributeValue av JOIN av.attribute a " +
            "WHERE pv.product.id = :productId AND (" +
            "LOWER(pv.sku) LIKE LOWER(concat('%', :searchTerm, '%')) OR " +
            "LOWER(a.name) LIKE LOWER(concat('%', :searchTerm, '%')) OR " +
            "LOWER(av.value) LIKE LOWER(concat('%', :searchTerm, '%')))")
    Page<ProductVariant> searchBySkuOrAttribute(@Param("productId") Long productId, @Param("searchTerm") String searchTerm, Pageable pageable);

    // Dùng để kiểm tra SKU đã tồn tại chưa (khi tạo/sửa)
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id); // Khi update, trừ chính nó ra
}