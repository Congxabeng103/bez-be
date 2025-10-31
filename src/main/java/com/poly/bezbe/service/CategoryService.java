package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.CategoryRequestDTO;
import com.poly.bezbe.dto.response.CategoryResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ cho Danh mục (Category).
 */
public interface CategoryService {

    /**
     * Lấy danh sách danh mục có phân trang, hỗ trợ lọc theo trạng thái
     * và tìm kiếm theo tên.
     *
     * @param pageable   Thông tin phân trang (page, size, sort).
     * @param searchTerm Từ khóa tìm kiếm (theo tên).
     * @param status     Trạng thái lọc ("ALL", "ACTIVE", "INACTIVE").
     * @return Đối tượng PageResponseDTO chứa danh sách danh mục và thông tin phân trang.
     */
    PageResponseDTO<CategoryResponseDTO> getAllCategories(Pageable pageable, String searchTerm, String status);

    /**
     * Lấy danh sách rút gọn các danh mục đang hoạt động (active = true).
     * Thường dùng để hiển thị trên các dropdown chọn.
     *
     * @return Danh sách CategoryResponseDTO.
     */
    List<CategoryResponseDTO> getAllCategoriesBrief();

    /**
     * Tạo một danh mục mới.
     *
     * @param request DTO chứa thông tin danh mục mới.
     * @return DTO của danh mục vừa được tạo.
     */
    CategoryResponseDTO createCategory(CategoryRequestDTO request);

    /**
     * Cập nhật thông tin một danh mục đã có.
     *
     * @param id      ID của danh mục cần cập nhật.
     * @param request DTO chứa thông tin cập nhật.
     * @return DTO của danh mục sau khi đã cập nhật.
     */
    CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request);

    /**
     * Xóa mềm (soft delete) một danh mục bằng cách chuyển trạng thái active = false.
     *
     * @param id ID của danh mục cần xóa.
     */
    void deleteCategory(Long id);
    void permanentDeleteCategory(Long id);
}