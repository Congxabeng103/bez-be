package com.poly.bezbe.controller;

import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.product.CategoryResponseDTO;
import com.poly.bezbe.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories") // Base path cho Category
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // API để lấy tất cả danh mục (cho dropdown)
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<CategoryResponseDTO>>> getAllCategories() {
        List<CategoryResponseDTO> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponseDTO.success(categories, "Lấy danh sách danh mục thành công"));
    }

    // (Bạn có thể thêm các endpoint POST, PUT, DELETE Category ở đây nếu cần)
}