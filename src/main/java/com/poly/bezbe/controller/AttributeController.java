package com.poly.bezbe.controller;


import com.poly.bezbe.dto.request.product.AttributeRequestDTO;
import com.poly.bezbe.dto.request.product.AttributeValueRequestDTO;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.product.AttributeResponseDTO;
import com.poly.bezbe.dto.response.product.AttributeValueResponseDTO;
import com.poly.bezbe.service.AttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attributes") // Base path cho Attribute
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;

    /**
     * Lấy tất cả thuộc tính và giá trị của chúng.
     * React sẽ gọi API này khi component mount.
     */
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<AttributeResponseDTO>>> getAllAttributes() {
        List<AttributeResponseDTO> attributes = attributeService.getAllAttributes();
        return ResponseEntity.ok(ApiResponseDTO.success(attributes, "Lấy danh sách thuộc tính thành công"));
    }

    /**
     * Tạo một thuộc tính mới.
     * React gọi khi nhấn nút "Tạo" trong form thêm thuộc tính.
     */
    @PostMapping
    public ResponseEntity<ApiResponseDTO<AttributeResponseDTO>> createAttribute(
            @Valid @RequestBody AttributeRequestDTO request) {
        AttributeResponseDTO newAttribute = attributeService.createAttribute(request);
        // Trả về CREATED (201) thay vì OK (200) cho thao tác tạo mới
        return new ResponseEntity<>(ApiResponseDTO.success(newAttribute, "Tạo thuộc tính thành công"), HttpStatus.CREATED);
    }

    /**
     * Xóa một thuộc tính (ví dụ: xóa "Màu sắc").
     * React gọi khi nhấn nút thùng rác cạnh tên thuộc tính.
     */
    @DeleteMapping("/{attributeId}")
    public ResponseEntity<ApiResponseDTO<Object>> deleteAttribute(@PathVariable Long attributeId) {
        attributeService.deleteAttribute(attributeId);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Xóa thuộc tính thành công"));
    }

    /**
     * Thêm một giá trị mới vào thuộc tính (ví dụ: thêm "Xanh" vào "Màu sắc").
     * React gọi khi nhấn nút "Thêm" trong form thêm giá trị.
     */
    @PostMapping("/{attributeId}/values")
    public ResponseEntity<ApiResponseDTO<AttributeValueResponseDTO>> addAttributeValue(
            @PathVariable Long attributeId,
            @Valid @RequestBody AttributeValueRequestDTO request) {
        AttributeValueResponseDTO newValue = attributeService.addAttributeValue(attributeId, request);
        return new ResponseEntity<>(ApiResponseDTO.success(newValue, "Thêm giá trị thành công"), HttpStatus.CREATED);
    }

    /**
     * Xóa một giá trị thuộc tính (ví dụ: xóa "Xanh").
     * React gọi khi nhấn nút "X" cạnh một giá trị.
     * Lưu ý: API endpoint là "/values/{valueId}" để tránh trùng với deleteAttribute.
     */
    @DeleteMapping("/values/{valueId}")
    public ResponseEntity<ApiResponseDTO<Object>> deleteAttributeValue(@PathVariable Long valueId) {
        attributeService.deleteAttributeValue(valueId);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Xóa giá trị thành công"));
    }
}