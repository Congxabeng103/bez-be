// File: com/poly/bezbe/repository/VariantOptionValueRepository.java
package com.poly.bezbe.repository;

import com.poly.bezbe.entity.VariantOptionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Set;

public interface VariantOptionValueRepository extends JpaRepository<VariantOptionValue, Long> {

    // (Tương tự hàm cũ)
    @Query("SELECT vov.optionValue.id FROM VariantOptionValue vov WHERE vov.variant.id = :variantId")
    Set<Long> findOptionValueIdsByVariantId(Long variantId);

    // (Hàm này phức tạp hơn, dùng để tìm variant theo tổ hợp)
    @Query("""
        SELECT vov.variant.id
        FROM VariantOptionValue vov
        WHERE vov.variant.product.id = :productId
          AND vov.optionValue.id IN :optionValueIds
        GROUP BY vov.variant.id
        HAVING COUNT(DISTINCT vov.optionValue.id) = :expectedCount
    """)
    List<Long> findVariantIdsByExactOptionValues(Long productId, List<Long> optionValueIds, Long expectedCount);


}