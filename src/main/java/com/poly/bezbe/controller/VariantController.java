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
import org.springframework.security.access.prepost.PreAuthorize; // <-- 1. IMPORT
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/variants")
@RequiredArgsConstructor
public class VariantController {

    private final VariantService variantService;

    // Lấy danh sách (Cả 3 vai trò đều được xem)
    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')") // <-- 2. THÊM
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<VariantResponseDTO>>> getVariantsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ALL") String status
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponseDTO<VariantResponseDTO> variants = variantService.getVariantsByProduct(productId, pageable, search, status);
        return ResponseEntity.ok(ApiResponseDTO.success(variants, "Lấy danh sách biến thể thành công"));
    }

    // Tạo mới (Chỉ Manager / Admin)
    @PostMapping("/batch")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')") // <-- 3. THÊM
    public ResponseEntity<ApiResponseDTO<List<VariantResponseDTO>>> createVariantsBatch(
            @Valid @RequestBody VariantBatchRequestDTO request) {
        List<VariantResponseDTO> newVariants = variantService.createVariantsBatch(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newVariants, "Tạo biến thể hàng loạt thành công"), HttpStatus.CREATED);
    }

    // Cập nhật (Chỉ Manager / Admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')") // <-- 4. THÊM
    public ResponseEntity<ApiResponseDTO<VariantResponseDTO>> updateVariant(
            @PathVariable Long id,
            @Valid @RequestBody VariantUpdateRequestDTO request) {
        VariantResponseDTO updatedVariant = variantService.updateVariant(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedVariant, "Cập nhật biến thể thành công"));
    }

    // Ngừng hoạt động (Chỉ Manager / Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')") // <-- 5. THÊM
    public ResponseEntity<ApiResponseDTO<Object>> deleteVariant(@PathVariable Long id) {
        variantService.deleteVariant(id); // Gọi Soft Delete
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động biến thể thành công"));
    }

    // Xóa vĩnh viễn (Chỉ Admin)
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasAuthority('ADMIN')") // <-- 6. THÊM
    public ResponseEntity<ApiResponseDTO<Object>> permanentDeleteVariant(@PathVariable Long id) {
        variantService.permanentDeleteVariant(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Đã xóa vĩnh viễn biến thể."));
    }

    // Tìm biến thể (Public - Dùng cho trang chi tiết sản phẩm)
    @GetMapping("/find")
    @PreAuthorize("permitAll()") // (Hoặc để trống nếu SecurityConfig đã mở)
    public ResponseEntity<ApiResponseDTO<VariantResponseDTO>> findVariantByAttributes(
            @RequestParam Long productId,
            @RequestParam List<Long> valueIds
    ) {
        VariantResponseDTO variant = variantService.findVariantByAttributes(productId, valueIds);
        return ResponseEntity.ok(ApiResponseDTO.success(variant, "Tìm thấy biến thể"));
    }
}