package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.product.ProductRequestDTO;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.product.ProductBriefDTO;
import com.poly.bezbe.dto.response.product.ProductDetailResponseDTO;
import com.poly.bezbe.dto.response.product.ProductResponseDTO;
import com.poly.bezbe.service.ProductService;
import com.poly.bezbe.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // <-- 1. IMPORT
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Lấy danh sách (Public - Cho phép tất cả)
    @GetMapping
    @PreAuthorize("permitAll()") // (Hoặc để trống nếu SecurityConfig đã mở)
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<ProductResponseDTO>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status,

            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice
    ) {
        // 1. Logic Sort (giữ nguyên)
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        // 2. Gọi Service với đầy đủ tham số
        PageResponseDTO<ProductResponseDTO> productPage = productService.getAllProducts(
                pageable, search, status, categoryName, minPrice, maxPrice
        );

        // 3. Trả về (giữ nguyên)
        return ResponseEntity.ok(ApiResponseDTO.success(productPage, "Lấy danh sách sản phẩm thành công"));
    }

    // Tạo mới (Chỉ Manager / Admin)
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')") // <-- 2. THÊM
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> createProduct(
            @Valid @RequestBody ProductRequestDTO request) {
        ProductResponseDTO newProduct = productService.createProduct(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newProduct, "Tạo sản phẩm thành công"), HttpStatus.CREATED);
    }

    // Cập nhật (Chỉ Manager / Admin)
    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')") // <-- 3. THÊM
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequestDTO request) {
        ProductResponseDTO updatedProduct = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedProduct, "Cập nhật sản phẩm thành công"));
    }

    // Ngừng hoạt động (Chỉ Manager / Admin)
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')") // <-- 4. THÊM
    public ResponseEntity<ApiResponseDTO<Object>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId); // Gọi hàm soft delete
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động sản phẩm thành công")); // Đổi message
    }

    // Lấy danh sách rút gọn (Cả 3 vai trò đều được xem)
    @GetMapping("/brief")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')") // <-- 5. THÊM
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<ProductBriefDTO>>> getProductBriefList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        PageResponseDTO<ProductBriefDTO> productPage = productService.getProductBriefList(pageable, search);
        return ResponseEntity.ok(ApiResponseDTO.success(productPage, "Lấy danh sách tóm tắt sản phẩm thành công"));
    }

    // Xóa vĩnh viễn (Chỉ Admin)
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasAuthority('ADMIN')") // <-- 6. THÊM
    public ResponseEntity<ApiResponseDTO<Object>> permanentDeleteProduct(@PathVariable Long id) {
        productService.permanentDeleteProduct(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Đã xóa vĩnh viễn sản phẩm."));
    }

    // Lấy chi tiết (Public - Cho phép tất cả)
    @GetMapping("/detail/{productId}")
    @PreAuthorize("permitAll()") // (Hoặc để trống nếu SecurityConfig đã mở)
    public ResponseEntity<ApiResponseDTO<ProductDetailResponseDTO>> getProductDetail(
            @PathVariable Long productId) {
        ProductDetailResponseDTO data = productService.getProductDetailById(productId);
        return ResponseEntity.ok(ApiResponseDTO.success(data, "Lấy chi tiết sản phẩm thành công"));
    }
}