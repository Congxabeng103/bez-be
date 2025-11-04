package com.poly.bezbe.service.impl; // <-- Chú ý package con 'impl'

import com.poly.bezbe.dto.request.CouponRequestDTO;
import com.poly.bezbe.dto.response.CouponResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.entity.Coupon;
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.DuplicateResourceException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.CouponRepository;
import com.poly.bezbe.service.CouponService; // <-- Import interface
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service // <-- @Service được đặt ở đây
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService { // <-- Implement interface

    private final CouponRepository couponRepository;

    // Hàm private này là chi tiết nội bộ, chỉ nằm ở Impl
    private CouponResponseDTO mapToCouponDTO(Coupon coupon) {
        return CouponResponseDTO.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountValue(coupon.getDiscountValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .minOrderAmount(coupon.getMinOrderAmount())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .active(coupon.isActive())
                .createdAt(coupon.getCreatedAt())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<CouponResponseDTO> getAllCoupons(Pageable pageable, String searchTerm, String status) {
        Page<Coupon> couponPage;
        boolean searching = searchTerm != null && !searchTerm.isBlank();
        boolean activeFilter = !"INACTIVE".equalsIgnoreCase(status);
        String search = searching ? searchTerm.trim() : null;

        couponPage = couponRepository.findBySearchAndStatus(search, status.toUpperCase(), activeFilter, pageable);

        List<CouponResponseDTO> dtos = couponPage.getContent().stream()
                .map(this::mapToCouponDTO).collect(Collectors.toList());
        return new PageResponseDTO<>(dtos, couponPage.getNumber(), couponPage.getSize(),
                couponPage.getTotalElements(), couponPage.getTotalPages());
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional
    public CouponResponseDTO createCoupon(CouponRequestDTO request) {
        String code = request.getCode().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Mã coupon '" + code + "' đã tồn tại.");
        }

        Coupon coupon = Coupon.builder()
                .code(code)
                .description(request.getDescription())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderAmount(request.getMinOrderAmount())
                .usageLimit(request.getUsageLimit() == null ? 0 : request.getUsageLimit()) // (Nếu 0 là không giới hạn)
                .usedCount(0) // Mới tạo
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(request.isActive())
                .build();
        Coupon saved = couponRepository.save(coupon);
        return mapToCouponDTO(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional
    public CouponResponseDTO updateCoupon(Long id, CouponRequestDTO request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Coupon: " + id));

        String code = request.getCode().trim().toUpperCase();
        if (!coupon.getCode().equalsIgnoreCase(code) &&
                couponRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new DuplicateResourceException("Mã coupon '" + code + "' đã được sử dụng.");
        }

        coupon.setCode(code);
        coupon.setDescription(request.getDescription());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setUsageLimit(request.getUsageLimit() == null ? 0 : request.getUsageLimit());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());
        coupon.setActive(request.isActive());

        Coupon updated = couponRepository.save(coupon);
        return mapToCouponDTO(updated);
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Coupon: " + id));

        coupon.setActive(false); // <-- SOFT DELETE
        couponRepository.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon validateCoupon(String code, BigDecimal subtotal) {
        if (code == null || code.trim().isEmpty()) {
            return null; // Không áp dụng coupon
        }

        // 1. Tìm coupon
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không hợp lệ"));

        // 2. Kiểm tra Active
        if (!coupon.isActive()) {
            throw new BusinessRuleException("Mã giảm giá đã hết hạn sử dụng");
        }

        // 3. Kiểm tra ngày
        LocalDate today = LocalDate.now();
        if (today.isBefore(coupon.getStartDate()) || today.isAfter(coupon.getEndDate())) {
            throw new BusinessRuleException("Mã giảm giá không nằm trong thời gian áp dụng");
        }

        // 4. Kiểm tra lượt dùng
        // (Giả sử 0 = không giới hạn)
        if (coupon.getUsageLimit() > 0 && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BusinessRuleException("Mã giảm giá đã hết lượt sử dụng");
        }

        // 5. Kiểm tra giá trị đơn hàng tối thiểu
        if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BusinessRuleException("Đơn hàng chưa đạt giá trị tối thiểu ("
                    + coupon.getMinOrderAmount() + "đ) để áp dụng mã");
        }

        return coupon;
    }
}