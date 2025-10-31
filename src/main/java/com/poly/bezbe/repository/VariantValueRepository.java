package com.poly.bezbe.repository;

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
}