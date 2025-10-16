package com.poly.bezbe.repository;

import com.poly.bezbe.entity.ProductVariantAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// Kế thừa JpaRepository đã cung cấp sẵn các hàm phân trang và sắp xếp như findAll(Pageable pageable)
public interface ProductVariantAttributeRepository extends JpaRepository<ProductVariantAttribute, Long> {
}