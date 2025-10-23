package com.poly.bezbe.service;


import com.poly.bezbe.dto.request.CouponRequestDTO;
import com.poly.bezbe.dto.response.CouponResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.entity.Coupon;
import com.poly.bezbe.exception.DuplicateResourceException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

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

    @Transactional(readOnly = true)
    public PageResponseDTO<CouponResponseDTO> getAllCoupons(Pageable pageable, String searchTerm) {
        Page<Coupon> couponPage;
        if (searchTerm != null && !searchTerm.isBlank()) {
            couponPage = couponRepository.findByCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    searchTerm.trim(), searchTerm.trim(), pageable);
        } else {
            couponPage = couponRepository.findAll(pageable);
        }

        List<CouponResponseDTO> dtos = couponPage.getContent().stream()
                .map(this::mapToCouponDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(dtos, couponPage.getNumber(), couponPage.getSize(),
                couponPage.getTotalElements(), couponPage.getTotalPages());
    }

    @Transactional
    public CouponResponseDTO createCoupon(CouponRequestDTO request) {
        if (couponRepository.existsByCode(request.getCode().trim())) {
            throw new DuplicateResourceException("Mã coupon '" + request.getCode().trim() + "' đã tồn tại.");
        }
        Coupon coupon = Coupon.builder()
                .code(request.getCode().trim())
                .description(request.getDescription())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderAmount(request.getMinOrderAmount())
                // Set default 0 for usedCount, handle null usageLimit
                .usageLimit(request.getUsageLimit() == null ? 0 : request.getUsageLimit()) // Giả sử 0 là không giới hạn
                .usedCount(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(request.isActive())
                .build();
        Coupon saved = couponRepository.save(coupon);
        return mapToCouponDTO(saved);
    }

    @Transactional
    public CouponResponseDTO updateCoupon(Long id, CouponRequestDTO request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Coupon: " + id));

        if (couponRepository.existsByCodeAndIdNot(request.getCode().trim(), id)) {
            throw new DuplicateResourceException("Mã coupon '" + request.getCode().trim() + "' đã được sử dụng.");
        }

        coupon.setCode(request.getCode().trim());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setUsageLimit(request.getUsageLimit() == null ? 0 : request.getUsageLimit());
        // Không cập nhật usedCount ở đây
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());
        coupon.setActive(request.isActive());

        Coupon updated = couponRepository.save(coupon);
        return mapToCouponDTO(updated);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        if (!couponRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy Coupon: " + id);
        }
        couponRepository.deleteById(id);
    }
}