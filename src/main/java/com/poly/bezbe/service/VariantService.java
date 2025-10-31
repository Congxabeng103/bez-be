package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.product.VariantBatchRequestDTO;
import com.poly.bezbe.dto.request.product.VariantUpdateRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.product.VariantResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến Biến thể sản phẩm (ProductVariant),
 * hay còn gọi là SKU.
 */
public interface VariantService {

    /**
     * Lấy danh sách Biến thể của một sản phẩm, có phân trang, lọc theo trạng thái
     * và tìm kiếm (theo SKU).
     *
     * @param productId  ID của sản phẩm cha.
     * @param pageable   Thông tin phân trang.
     * @param searchTerm Từ khóa tìm kiếm (SKU).
     * @param status     Trạng thái lọc ("ALL", "ACTIVE", "INACTIVE").
     * @return PageResponseDTO chứa danh sách biến thể.
     */
    PageResponseDTO<VariantResponseDTO> getVariantsByProduct(Long productId, Pageable pageable, String searchTerm, String status);

    /**
     * Tạo một loạt các biến thể mới cho một sản phẩm từ một DTO hàng loạt.
     *
     * @param batchRequest DTO chứa ID sản phẩm và danh sách các biến thể cần tạo.
     * @return Danh sách các DTO của biến thể vừa được tạo.
     */
    List<VariantResponseDTO> createVariantsBatch(VariantBatchRequestDTO batchRequest);

    /**
     * Cập nhật thông tin của một biến thể cụ thể (giá, kho, ảnh, SKU, trạng thái).
     *
     * @param variantId ID của biến thể cần cập nhật.
     * @param request   DTO chứa thông tin cập nhật.
     * @return DTO của biến thể sau khi cập nhật.
     */
    VariantResponseDTO updateVariant(Long variantId, VariantUpdateRequestDTO request);

    /**
     * Xóa mềm (soft delete) một biến thể (chuyển active = false).
     *
     * @param variantId ID của biến thể cần xóa.
     */
    void deleteVariant(Long variantId);
    // --- THÊM HÀM MỚI NÀY ---
    /**
     * Xóa vĩnh viễn (hard delete) một biến thể.
     * Chỉ thành công nếu biến thể không có trong bất kỳ chi tiết đơn hàng nào.
     *
     * @param variantId ID của biến thể cần xóa.
     */
    void permanentDeleteVariant(Long variantId);
}