package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Attribute;
import com.poly.bezbe.entity.AttributeValue;
import com.poly.bezbe.entity.VariantValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set; // Import Set

@Repository
public interface VariantValueRepository extends JpaRepository<VariantValue, Long> {

    // Lấy tất cả các cặp giá trị thuộc tính của một biến thể
    Set<VariantValue> findByVariant_Id(Long variantId); // Trả về Set để mapToVariantDTO
    @Query("SELECT vv.attributeValue.id FROM VariantValue vv WHERE vv.variant.id = :variantId")
    Set<Long> findAttributeValueIdsByVariantId(@Param("variantId") Long variantId);
    // --- THÊM 2 HÀM NÀY VÀO (Sửa lỗi 2 và 3) ---

    /**
     * Lỗi 2: Lấy các Thuộc tính (Attribute) của 1 Sản phẩm
     * (Tìm các thuộc tính (Size, Color) mà sản phẩm này có biến thể)
     */
    @Query("SELECT DISTINCT vv.attributeValue.attribute FROM VariantValue vv " +
            "WHERE vv.variant.product.id = :productId")
    Set<Attribute> findAttributesByProductId(@Param("productId") Long productId);

    /**
     * Lỗi 3: Lấy các Giá trị (AttributeValue) của 1 Sản phẩm VÀ 1 Thuộc tính
     * (Tìm các giá trị (S, M, L) của thuộc tính "Size" mà sản phẩm này có)
     */
    @Query("SELECT DISTINCT vv.attributeValue FROM VariantValue vv " +
            "WHERE vv.variant.product.id = :productId AND vv.attributeValue.attribute.id = :attributeId")
    Set<AttributeValue> findAttributeValuesByProductIdAndAttributeId(
            @Param("productId") Long productId,
            @Param("attributeId") Long attributeId
    );
    // --- KẾT THÚC THÊM ---
    /**
     * HÀM MỚI: Tìm ID biến thể dựa trên danh sách Value ID
     */
    @Query("SELECT vv.variant.id FROM VariantValue vv " +
            "WHERE vv.attributeValue.id IN :valueIds " +
            "AND vv.variant.product.id = :productId " +
            "GROUP BY vv.variant.id " +
            "HAVING COUNT(DISTINCT vv.attributeValue.id) = :idCount")
    List<Long> findVariantIdsByExactAttributeValues(
            @Param("productId") Long productId,
            @Param("valueIds") List<Long> valueIds,
            @Param("idCount") long idCount
    );
    // --- KẾT THÚC THÊM ---
}