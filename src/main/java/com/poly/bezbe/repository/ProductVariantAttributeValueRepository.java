package com.poly.bezbe.repository;

import com.poly.bezbe.entity.ProductVariantAttributeValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantAttributeValueRepository extends JpaRepository<ProductVariantAttributeValue, Long> {
    /**
     * Tự động tạo câu lệnh tìm tất cả các giá trị thuộc về một thuộc tính cụ thể,
     * có hỗ trợ phân trang và sắp xếp.
     * @param attributeId ID của thuộc tính cha
     * @param pageable Đối tượng chứa thông tin phân trang (trang số mấy, bao nhiêu phần tử) và sắp xếp
     * @return Một trang (Page) chứa các giá trị thuộc tính
     */
    Page<ProductVariantAttributeValue> findAllByAttributeId(Long attributeId, Pageable pageable);
}