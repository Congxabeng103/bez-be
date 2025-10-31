package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.product.VariantBatchRequestDTO;
import com.poly.bezbe.dto.request.product.VariantUpdateRequestDTO;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.product.VariantResponseDTO;
import com.poly.bezbe.service.VariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/variants")
@RequiredArgsConstructor
public class VariantController {

    private final VariantService variantService;

    // Lấy danh sách biến thể theo ID sản phẩm (có lọc status)
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<VariantResponseDTO>>> getVariantsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ALL") String status // <-- Thêm status
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponseDTO<VariantResponseDTO> variants = variantService.getVariantsByProduct(productId, pageable, search, status);
        return ResponseEntity.ok(ApiResponseDTO.success(variants, "Lấy danh sách biến thể thành công"));
    }

    // Tạo biến thể hàng loạt
    @PostMapping("/batch")
    public ResponseEntity<ApiResponseDTO<List<VariantResponseDTO>>> createVariantsBatch(
            @Valid @RequestBody VariantBatchRequestDTO request) {
        List<VariantResponseDTO> newVariants = variantService.createVariantsBatch(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newVariants, "Tạo biến thể hàng loạt thành công"), HttpStatus.CREATED);
    }

    // Cập nhật 1 biến thể (SKU, giá, kho, ảnh, trạng thái)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<VariantResponseDTO>> updateVariant(
            @PathVariable Long id,
            @Valid @RequestBody VariantUpdateRequestDTO request) {
        VariantResponseDTO updatedVariant = variantService.updateVariant(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedVariant, "Cập nhật biến thể thành công"));
    }

    // Xóa mềm 1 biến thể
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Object>> deleteVariant(@PathVariable Long id) {
        variantService.deleteVariant(id); // Gọi Soft Delete
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động biến thể thành công"));
    }
    // --- THÊM ENDPOINT MỚI NÀY ---
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponseDTO<Object>> permanentDeleteVariant(@PathVariable Long id) {
        variantService.permanentDeleteVariant(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Đã xóa vĩnh viễn biến thể."));
    }
}