package com.poly.bezbe.controller;


import com.poly.bezbe.dto.request.product.ProductRequestDTO;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.product.ProductBriefDTO;
import com.poly.bezbe.dto.response.product.ProductResponseDTO;
import com.poly.bezbe.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products") // Đặt base path cho tất cả API sản phẩm
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<ProductResponseDTO>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page, // Trang (Spring bắt đầu từ 0)
            @RequestParam(defaultValue = "5") int size, // Kích thước trang
            @RequestParam(required = false) String search, // Từ khóa tìm kiếm
            @RequestParam(defaultValue = "createdAt,desc") String sort // Sắp xếp
    ) {
        // Tách chuỗi sort (ví dụ: "createdAt,desc")
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PageResponseDTO<ProductResponseDTO> productPage = productService.getAllProducts(pageable, search);

        // Trả về theo cấu trúc ApiResponseDTO
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

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponseDTO<Object>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Xóa sản phẩm thành công"));
    }
    @GetMapping("/brief")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<ProductBriefDTO>>> getProductBriefList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, // Tăng size cho dropdown
            @RequestParam(required = false) String search
    ) {
        // Sắp xếp theo tên để dễ tìm trong dropdown
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        PageResponseDTO<ProductBriefDTO> productPage = productService.getProductBriefList(pageable, search); // Gọi hàm mới trong Service
        return ResponseEntity.ok(ApiResponseDTO.success(productPage, "Lấy danh sách tóm tắt sản phẩm thành công"));
    }
    // (Bạn sẽ cần API để lấy Category và Brand cho Form, ví dụ:)
    // @GetMapping("/categories")
    // public ResponseEntity<ApiResponseDTO<List<CategoryResponseDTO>>> getAllCategories() { ... }

    // @GetMapping("/brands")
    // public ResponseEntity<ApiResponseDTO<List<BrandResponseDTO>>> getAllBrands() { ... }
}