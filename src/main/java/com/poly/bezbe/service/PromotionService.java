package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.PromotionRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.PromotionBriefDTO;
import com.poly.bezbe.dto.response.PromotionResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ cho Khuyến mãi (Promotion).
 */
public interface PromotionService {

    /**
     * Lấy danh sách khuyến mãi có phân trang, hỗ trợ lọc theo trạng thái
     * và tìm kiếm theo tên.
     *
     * @param pageable   Thông tin phân trang.
     * @param searchTerm Từ khóa tìm kiếm (theo tên).
     * @param status     Trạng thái lọc (ví dụ: "ALL", "ACTIVE", "INACTIVE", "EXPIRED", "UPCOMING").
     * @return PageResponseDTO chứa danh sách khuyến mãi.
     */
    PageResponseDTO<PromotionResponseDTO> getAllPromotions(Pageable pageable, String searchTerm, String status);

    /**
     * Tạo một khuyến mãi mới.
     *
     * @param request DTO chứa thông tin khuyến mãi mới.
     * @return DTO của khuyến mãi vừa tạo.
     */
    PromotionResponseDTO createPromotion(PromotionRequestDTO request);

    /**
     * Cập nhật thông tin một khuyến mãi.
     *
     * @param id      ID của khuyến mãi cần cập nhật.
     * @param request DTO chứa thông tin cập nhật.
     * @return DTO của khuyến mãi sau khi cập nhật.
     */
    PromotionResponseDTO updatePromotion(Long id, PromotionRequestDTO request);

    /**
     * Xóa mềm (soft delete) một khuyến mãi (chuyển active = false).
     *
     * @param id ID của khuyến mãi cần xóa.
     */
    void deletePromotion(Long id);

    /**
     * Lấy danh sách rút gọn các khuyến mãi đang hoạt động (active = true).
     * Thường dùng để hiển thị trên các dropdown chọn.
     *
     * @return Danh sách PromotionBriefDTO.
     */
    List<PromotionBriefDTO> getPromotionBriefList();
    void permanentDeletePromotion(Long id);
}