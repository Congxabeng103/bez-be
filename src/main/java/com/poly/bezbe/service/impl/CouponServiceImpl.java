package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.CouponRequestDTO;
import com.poly.bezbe.dto.response.CouponResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.entity.Coupon;
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.DuplicateResourceException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.CouponRepository;
import com.poly.bezbe.service.CouponService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

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

    @Override
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
     * (HÀM HELPER)
     * Ném lỗi nếu admin cố kích hoạt coupon không hợp lệ.
     */
    private boolean determineActiveStatus(LocalDate startDate, LocalDate endDate, boolean formIsActive) {
        if (!formIsActive) {
            return false;
        }
        LocalDate today = LocalDate.now();

        if (endDate.isBefore(today)) {
            throw new BusinessRuleException("Lỗi: Không thể kích hoạt. Ngày kết thúc đã ở trong quá khứ.");
        }

        if (startDate.isAfter(today)) {
            throw new BusinessRuleException("Lỗi: Không thể kích hoạt. Ngày bắt đầu là ở trong tương lai. (Bỏ tick 'Kích hoạt' để lưu nháp)");
        }

        return true;
    }

    @Override
    @Transactional
    public CouponResponseDTO createCoupon(CouponRequestDTO request) {
        String code = request.getCode().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Mã coupon '" + code + "' đã tồn tại.");
        }

        boolean newActiveStatus = determineActiveStatus(
                request.getStartDate(),
                request.getEndDate(),
                request.isActive()
        );

        Coupon coupon = Coupon.builder()
                .code(code)
                .description(request.getDescription())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderAmount(request.getMinOrderAmount())
                .usageLimit(request.getUsageLimit() == null ? 0 : request.getUsageLimit())
                .usedCount(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(newActiveStatus) // Dùng trạng thái đã validate
                .build();
        Coupon saved = couponRepository.save(coupon);
        return mapToCouponDTO(saved);
    }

    @Override
    @Transactional
    public CouponResponseDTO updateCoupon(Long id, CouponRequestDTO request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Coupon: " + id));

        String code = request.getCode().trim().toUpperCase();
        if (!coupon.getCode().equalsIgnoreCase(code) &&
                couponRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new DuplicateResourceException("Mã coupon '" + code + "' đã được sử dụng.");
        }

        boolean newActiveStatus = determineActiveStatus(
                request.getStartDate(),
                request.getEndDate(),
                request.isActive()
        );

        coupon.setCode(code);
        coupon.setDescription(request.getDescription());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setUsageLimit(request.getUsageLimit() == null ? 0 : request.getUsageLimit());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());
        coupon.setActive(newActiveStatus); // Dùng trạng thái đã validate

        Coupon updated = couponRepository.save(coupon);
        return mapToCouponDTO(updated);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Coupon: " + id));
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon validateCoupon(String code, BigDecimal subtotal) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không hợp lệ"));
        if (!coupon.isActive()) {
            throw new BusinessRuleException("Mã giảm giá đã hết hạn sử dụng");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(coupon.getStartDate()) || today.isAfter(coupon.getEndDate())) {
            throw new BusinessRuleException("Mã giảm giá không nằm trong thời gian áp dụng");
        }
        if (coupon.getUsageLimit() > 0 && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BusinessRuleException("Mã giảm giá đã hết lượt sử dụng");
        }
        if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BusinessRuleException("Đơn hàng chưa đạt giá trị tối thiểu ("
                    + coupon.getMinOrderAmount() + "đ) để áp dụng mã");
        }
        return coupon;
    }

    @Override
    @Transactional
    public void permanentDeleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy coupon với ID: " + id));
        if (coupon.getUsedCount() > 0) {
            throw new IllegalStateException("Không thể xóa vĩnh viễn coupon đã có lượt sử dụng.");
        }
        couponRepository.delete(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponseDTO> getPublicActiveCoupons(int size) {
        Pageable pageable = PageRequest.of(0, size, Sort.by("endDate").ascending());
        List<Coupon> coupons = couponRepository.findByActive(true, pageable);
        return coupons.stream()
                .map(this::mapToCouponDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponseDTO> getAllPublicActiveCoupons() {
        List<Coupon> coupons = couponRepository.findAllByActive(true);
        return coupons.stream()
                .map(this::mapToCouponDTO)
                .collect(Collectors.toList());
    }
}