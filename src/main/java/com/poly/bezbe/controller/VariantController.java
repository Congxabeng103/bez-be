package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.product.BatchCreateVariantsRequestDTO;
import com.poly.bezbe.dto.request.product.UpdateVariantRequestDTO;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO; // Import
import com.poly.bezbe.dto.response.product.VariantResponseDTO;
import com.poly.bezbe.service.VariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest; // Import
import org.springframework.data.domain.Pageable; // Import
import org.springframework.data.domain.Sort; // Import
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/variants") // Base path cho Variant
@RequiredArgsConstructor
public class VariantController {

    private final VariantService variantService;

    /**
     * API để tạo nhiều biến thể cùng lúc (batch).
     * React gọi khi nhấn "Lưu tất cả biến thể".
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponseDTO<List<VariantResponseDTO>>> createVariantsInBatch(
            @Valid @RequestBody BatchCreateVariantsRequestDTO request) {
        List<VariantResponseDTO> newVariants = variantService.createVariantsInBatch(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newVariants, "Tạo biến thể thành công"), HttpStatus.CREATED);
    }

    /**
     * Lấy danh sách biến thể của một sản phẩm (có phân trang và tìm kiếm).
     * React gọi khi chọn "Xem biến thể" của một sản phẩm.
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<VariantResponseDTO>>> getVariantsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PageResponseDTO<VariantResponseDTO> variantPage = variantService.getVariantsByProduct(productId, pageable, search);
        return ResponseEntity.ok(ApiResponseDTO.success(variantPage, "Lấy danh sách biến thể thành công"));
    }

    /**
     * Cập nhật thông tin một biến thể (không cập nhật thuộc tính).
     * React gọi khi nhấn "Lưu" trong modal chỉnh sửa.
     */
    @PutMapping("/{variantId}")
    public ResponseEntity<ApiResponseDTO<VariantResponseDTO>> updateVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody UpdateVariantRequestDTO request) {
        VariantResponseDTO updatedVariant = variantService.updateVariant(variantId, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedVariant, "Cập nhật biến thể thành công"));
    }

    /**
     * Xóa một biến thể.
     * React gọi khi nhấn nút thùng rác trên một dòng biến thể.
     */
    @DeleteMapping("/{variantId}")
    public ResponseEntity<ApiResponseDTO<Object>> deleteVariant(@PathVariable Long variantId) {
        variantService.deleteVariant(variantId);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Xóa biến thể thành công"));
    }
}