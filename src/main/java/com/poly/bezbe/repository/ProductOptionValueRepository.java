// File: com/poly/bezbe/repository/ProductOptionValueRepository.java
package com.poly.bezbe.repository;

import com.poly.bezbe.entity.ProductOptionValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductOptionValueRepository extends JpaRepository<ProductOptionValue, Long> {
    // Tìm các giá trị theo Option ID
    List<ProductOptionValue> findByOption_Id(Long optionId);
}