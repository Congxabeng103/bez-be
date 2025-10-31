package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.product.AttributeRequestDTO;
import com.poly.bezbe.dto.request.product.AttributeValueRequestDTO;
import com.poly.bezbe.dto.response.product.AttributeResponseDTO;
import com.poly.bezbe.dto.response.product.AttributeValueResponseDTO;

import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến Thuộc tính sản phẩm.
 */
public interface AttributeService {

    /**
     * Lấy tất cả thuộc tính và giá trị của chúng.
     */
    List<AttributeResponseDTO> getAllAttributes();

    /**
     * Tạo một thuộc tính mới (ví dụ: "Chất liệu").
     */
    AttributeResponseDTO createAttribute(AttributeRequestDTO request);

    /**
     * Xóa một thuộc tính và tất cả giá trị con của nó.
     */
    void deleteAttribute(Long attributeId);

    /**
     * Thêm một giá trị mới vào thuộc tính (ví dụ: thêm "Cotton" vào "Chất liệu").
     */
    AttributeValueResponseDTO addAttributeValue(Long attributeId, AttributeValueRequestDTO request);

    /**
     * Xóa một giá trị thuộc tính.
     */
    void deleteAttributeValue(Long valueId);
}