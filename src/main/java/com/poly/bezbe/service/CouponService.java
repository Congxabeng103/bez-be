package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.CouponRequestDTO;
import com.poly.bezbe.dto.response.CouponResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.entity.Coupon;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Interface định nghĩa các nghiệp vụ cho Mã giảm giá (Coupon).
 */
public interface CouponService {

    /**
     * Lấy danh sách mã giảm giá có phân trang, hỗ trợ lọc theo trạng thái
     * và tìm kiếm theo mã (code).
     *
     * @param pageable   Thông tin phân trang.
     * @param searchTerm Từ khóa tìm kiếm (theo mã).
     * @param status     Trạng thái lọc (ví dụ: "ALL", "ACTIVE", "INACTIVE", "EXPIRED", "UPCOMING").
     * @return PageResponseDTO chứa danh sách coupons.
     */
    PageResponseDTO<CouponResponseDTO> getAllCoupons(Pageable pageable, String searchTerm, String status);

    /**
     * Tạo một mã giảm giá mới.
     *
     * @param request DTO chứa thông tin mã mới.
     * @return DTO của mã vừa tạo.
     */
    CouponResponseDTO createCoupon(CouponRequestDTO request);

    /**
     * Cập nhật thông tin một mã giảm giá.
     *
     * @param id      ID của mã cần cập nhật.
     * @param request DTO chứa thông tin cập nhật.
     * @return DTO của mã sau khi cập nhật.
     */
    CouponResponseDTO updateCoupon(Long id, CouponRequestDTO request);

    /**
     * Xóa mềm (soft delete) một mã giảm giá (chuyển active = false).
     *
     * @param id ID của mã cần xóa.
     */
    void deleteCoupon(Long id);
    Coupon validateCoupon(String code, BigDecimal subtotal);
}