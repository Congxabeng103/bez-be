package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.product.BrandRequestDTO;
import com.poly.bezbe.dto.response.product.BrandResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ cho Thương hiệu (Brand).
 */
public interface BrandService {

    /**
     * Lấy danh sách thương hiệu có phân trang, hỗ trợ lọc theo trạng thái
     * và tìm kiếm theo tên.
     *
     * @param pageable   Thông tin phân trang (page, size, sort).
     * @param searchTerm Từ khóa tìm kiếm (theo tên).
     * @param status     Trạng thái lọc ("ALL", "ACTIVE", "INACTIVE").
     * @return Đối tượng PageResponseDTO chứa danh sách thương hiệu và thông tin phân trang.
     */
    PageResponseDTO<BrandResponseDTO> getAllBrands(Pageable pageable, String searchTerm, String status);

    /**
     * Lấy danh sách rút gọn các thương hiệu đang hoạt động (active = true).
     * Thường dùng để hiển thị trên các dropdown chọn.
     *
     * @return Danh sách BrandResponseDTO.
     */
    List<BrandResponseDTO> getAllBrandsBrief();

    /**
     * Tạo một thương hiệu mới.
     *
     * @param request DTO chứa thông tin thương hiệu mới.
     * @return DTO của thương hiệu vừa được tạo.
     */
    BrandResponseDTO createBrand(BrandRequestDTO request);

    /**
     * Cập nhật thông tin một thương hiệu đã có.
     *
     * @param id      ID của thương hiệu cần cập nhật.
     * @param request DTO chứa thông tin cập nhật.
     * @return DTO của thương hiệu sau khi đã cập nhật.
     */
    BrandResponseDTO updateBrand(Long id, BrandRequestDTO request);

    /**
     * Xóa mềm (soft delete) một thương hiệu bằng cách chuyển trạng thái active = false.
     *
     * @param id ID của thương hiệu cần xóa.
     */
    void deleteBrand(Long id);
    void permanentDeleteBrand(Long id);
}