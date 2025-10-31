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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // --- SỬA HÀM NÀY (Thêm 'status') ---
    // --- SỬA LẠI HOÀN TOÀN HÀM NÀY ---
    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<ProductResponseDTO>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size, // <-- Tăng size mặc định lên 12 cho khớp frontend
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status, // Trang shop luôn gửi "ACTIVE"

            // --- THÊM 3 THAM SỐ TỪ REACT ---
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
    @PostMapping
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> createProduct(
            @Valid @RequestBody ProductRequestDTO request) {
        ProductResponseDTO newProduct = productService.createProduct(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newProduct, "Tạo sản phẩm thành công"), HttpStatus.CREATED);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequestDTO request) {
        ProductResponseDTO updatedProduct = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedProduct, "Cập nhật sản phẩm thành công"));
    }

    // --- SỬA HÀM NÀY (Để gọi Soft Delete) ---
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponseDTO<Object>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId); // Gọi hàm soft delete
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động sản phẩm thành công")); // Đổi message
    }

    @GetMapping("/brief")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<ProductBriefDTO>>> getProductBriefList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        // Hàm getProductBriefList đã được sửa ở Service để chỉ lấy Active
        PageResponseDTO<ProductBriefDTO> productPage = productService.getProductBriefList(pageable, search);
        return ResponseEntity.ok(ApiResponseDTO.success(productPage, "Lấy danh sách tóm tắt sản phẩm thành công"));
    }

    // THÊM ENDPOINT MỚI
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponseDTO<Object>> permanentDeleteProduct(@PathVariable Long id) {
        productService.permanentDeleteProduct(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Đã xóa vĩnh viễn sản phẩm."));
    }
    // --- THÊM ENDPOINT MỚI NÀY ---
    // (Đặt endpoint này TRƯỚC /brief để tránh xung đột)
    @GetMapping("/detail/{productId}")
    public ResponseEntity<ApiResponseDTO<ProductDetailResponseDTO>> getProductDetail(
            @PathVariable Long productId) {
        ProductDetailResponseDTO data = productService.getProductDetailById(productId);
        return ResponseEntity.ok(ApiResponseDTO.success(data, "Lấy chi tiết sản phẩm thành công"));
    }
    // --- KẾT THÚC ENDPOINT MỚI ---
}