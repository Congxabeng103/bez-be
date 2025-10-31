package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.CategoryRequestDTO;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.CategoryResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // --- SỬA HÀM NÀY (Thêm @RequestParam 'status') ---
    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<CategoryResponseDTO>>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(defaultValue = "ALL") String status // <-- THÊM DÒNG NÀY (Mặc định là ALL)
    ) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        // Truyền 'status' vào service
        PageResponseDTO<CategoryResponseDTO> categoryPage = categoryService.getAllCategories(pageable, search, status);
        return ResponseEntity.ok(ApiResponseDTO.success(categoryPage, "Lấy danh sách danh mục thành công"));
    }

    // GET Brief (Lấy tất cả, cho dropdown)
    @GetMapping("/all-brief")
    public ResponseEntity<ApiResponseDTO<List<CategoryResponseDTO>>> getAllCategoriesBrief() {
        List<CategoryResponseDTO> categories = categoryService.getAllCategoriesBrief();
        return ResponseEntity.ok(ApiResponseDTO.success(categories, "Lấy danh sách tóm tắt danh mục thành công"));
    }

    // POST (Tạo mới)
    @PostMapping
    public ResponseEntity<ApiResponseDTO<CategoryResponseDTO>> createCategory(
            @Valid @RequestBody CategoryRequestDTO request) {
        CategoryResponseDTO newCategory = categoryService.createCategory(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newCategory, "Tạo danh mục thành công"), HttpStatus.CREATED);
    }

    // PUT (Cập nhật)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<CategoryResponseDTO>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDTO request) {
        CategoryResponseDTO updatedCategory = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedCategory, "Cập nhật danh mục thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Object>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id); // Hàm này giờ sẽ ẩn hàng loạt
        // Sửa lại thông báo
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động danh mục và các sản phẩm liên quan thành công"));
    }
    // --- THÊM ENDPOINT MỚI ---
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponseDTO<Object>> permanentDeleteCategory(@PathVariable Long id) {
        categoryService.permanentDeleteCategory(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Đã xóa vĩnh viễn danh mục."));
    }
}