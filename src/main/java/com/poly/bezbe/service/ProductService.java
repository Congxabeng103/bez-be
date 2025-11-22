package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.product.ProductRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.product.ProductBriefDTO;
import com.poly.bezbe.dto.response.product.ProductDetailResponseDTO;
import com.poly.bezbe.dto.response.product.ProductResponseDTO;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến Sản phẩm (Product).
 */
public interface ProductService {

    /**
     * Lấy danh sách sản phẩm (admin view) có phân trang, lọc theo trạng thái
     * và tìm kiếm theo tên.
     *
     * @param pageable   Thông tin phân trang.
     * @param searchTerm Từ khóa tìm kiếm (theo tên).
     * @param status     Trạng thái lọc ("ALL", "ACTIVE", "INACTIVE").
     * @return PageResponseDTO chứa danh sách sản phẩm.
     */
    PageResponseDTO<ProductResponseDTO> getAllProducts(
            Pageable pageable,
            String searchTerm,
            String status,
            String categoryName, // <-- Thêm
            Double minPrice,
            Double maxPrice,
            Boolean hasVariants,
            String brandName,
            Long promotionId
    );
    /**
     * Tạo một sản phẩm mới.
     *
     * @param request DTO chứa thông tin sản phẩm.
     * @return DTO của sản phẩm vừa tạo.
     */
    ProductResponseDTO createProduct(ProductRequestDTO request);

    /**
     * Cập nhật thông tin một sản phẩm.
     *
     * @param productId ID của sản phẩm cần cập nhật.
     * @param request   DTO chứa thông tin cập nhật.
     * @return DTO của sản phẩm sau khi cập nhật.
     */
    ProductResponseDTO updateProduct(Long productId, ProductRequestDTO request);

    /**
     * Xóa mềm (soft delete) một sản phẩm (chuyển active = false).
     *
     * @param productId ID của sản phẩm cần xóa.
     */
    void deleteProduct(Long productId);

    /**
     * Lấy danh sách tóm tắt (brief) các sản phẩm đang hoạt động.
     * Thường dùng để admin chọn sản phẩm khi thêm biến thể.
     *
     * @param pageable   Thông tin phân trang.
     * @param searchTerm Từ khóa tìm kiếm (theo tên).
     * @return PageResponseDTO chứa danh sách tóm tắt.
     */
    PageResponseDTO<ProductBriefDTO> getProductBriefList(Pageable pageable, String searchTerm);
    // THÊM HÀM NÀY
    void permanentDeleteProduct(Long productId);
    ProductDetailResponseDTO getProductDetailById(Long productId);
    BigDecimal getHighestProductPrice();
}