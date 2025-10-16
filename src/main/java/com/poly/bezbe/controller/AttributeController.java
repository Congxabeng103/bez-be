package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.attribute.AttributeRequestDTO;
import com.poly.bezbe.dto.request.attribute.AttributeValueRequest;
import com.poly.bezbe.dto.response.attribute.AttributeResponseDTO;
import com.poly.bezbe.dto.response.attribute.AttributeValueResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.service.AttributeService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/attributes")
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;

    // --- API CHO THUỘC TÍNH (ATTRIBUTE) ---

    /**
     * Lấy danh sách tất cả thuộc tính (có phân trang và sắp xếp).
     * Ví dụ: /api/v1/admin/attributes?page=0&size=10&sort=name,asc
     */
    @GetMapping
    public ResponseEntity<PageResponseDTO<AttributeResponseDTO>> getAllAttributes(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(attributeService.getAllAttributes(pageable));
    }

    @PostMapping
    public ResponseEntity<AttributeResponseDTO> createAttribute(@RequestBody AttributeRequestDTO request) {
        return ResponseEntity.ok(attributeService.createAttribute(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttributeResponseDTO> getAttributeById(@PathVariable Long id) {
        return ResponseEntity.ok(attributeService.getAttributeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttributeResponseDTO> updateAttribute(@PathVariable Long id, @RequestBody AttributeRequestDTO request) {
        return ResponseEntity.ok(attributeService.updateAttribute(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAttribute(@PathVariable Long id) {
        attributeService.deleteAttribute(id);
        return ResponseEntity.ok("Xóa thuộc tính thành công!");
    }

    // --- API CHO GIÁ TRỊ THUỘC TÍNH (ATTRIBUTE VALUE) ---

    /**
     * Lấy danh sách giá trị của một thuộc tính (có phân trang và sắp xếp).
     * Ví dụ: /api/v1/admin/attributes/1/values?page=0&size=5&sort=value,desc
     */
    @GetMapping("/{attributeId}/values")
    public ResponseEntity<PageResponseDTO<AttributeValueResponseDTO>> getValuesForAttribute(
            @PathVariable Long attributeId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(attributeService.getValuesForAttribute(attributeId, pageable));
    }

    @PostMapping("/{attributeId}/values")
    public ResponseEntity<AttributeValueResponseDTO> createAttributeValue(
            @PathVariable Long attributeId,
            @RequestBody AttributeValueRequest request) {
        return ResponseEntity.ok(attributeService.createAttributeValue(attributeId, request));
    }

    @DeleteMapping("/values/{valueId}")
    public ResponseEntity<String> deleteAttributeValue(@PathVariable Long valueId) {
        attributeService.deleteAttributeValue(valueId);
        return ResponseEntity.ok("Xóa giá trị thuộc tính thành công!");
    }
}