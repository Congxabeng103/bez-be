package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.product.BrandRequestDTO; // (DTO của bạn)
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.product.BrandResponseDTO; // (DTO của bạn)
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // <-- IMPORT
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    // Lấy danh sách cho trang Admin (Cả 3 vai trò đều được xem)
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<BrandResponseDTO>>> getAllBrands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(defaultValue = "ALL") String status
    ) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PageResponseDTO<BrandResponseDTO> brandPage = brandService.getAllBrands(pageable, search, status);
        return ResponseEntity.ok(ApiResponseDTO.success(brandPage, "Lấy danh sách thương hiệu thành công"));
    }

    // GET Brief (Public - Cho phép tất cả, kể cả user)
    @GetMapping("/all-brief")
    @PreAuthorize("permitAll()") // (Hoặc để trống nếu SecurityConfig đã mở)
    public ResponseEntity<ApiResponseDTO<List<BrandResponseDTO>>> getAllBrandsBrief() {
        List<BrandResponseDTO> brands = brandService.getAllBrandsBrief();
        return ResponseEntity.ok(ApiResponseDTO.success(brands, "Lấy danh sách tóm tắt thương hiệu thành công"));
    }

    // POST (Tạo mới - Chỉ Manager / Admin)
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponseDTO<BrandResponseDTO>> createBrand(
            @Valid @RequestBody BrandRequestDTO request) {
        BrandResponseDTO newBrand = brandService.createBrand(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newBrand, "Tạo thương hiệu thành công"), HttpStatus.CREATED);
    }

    // PUT (Cập nhật - Chỉ Manager / Admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponseDTO<BrandResponseDTO>> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody BrandRequestDTO request) {
        BrandResponseDTO updatedBrand = brandService.updateBrand(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedBrand, "Cập nhật thương hiệu thành công"));
    }

    // Ngừng hoạt động (Chỉ Manager / Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponseDTO<Object>> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động thương hiệu và các sản phẩm liên quan thành công"));
    }

    // Xóa vĩnh viễn (Chỉ Admin)
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Object>> permanentDeleteBrand(@PathVariable Long id) {
        brandService.permanentDeleteBrand(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Đã xóa vĩnh viễn thương hiệu."));
    }
}