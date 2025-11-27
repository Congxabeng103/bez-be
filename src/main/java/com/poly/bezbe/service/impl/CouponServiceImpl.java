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
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
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
     * SỬA LẠI LOGIC CHECK TRẠNG THÁI KHI TẠO/SỬA
     * - Dùng múi giờ VN.
     * - Cho phép startDate ở tương lai (để lên lịch).
     * - Chỉ chặn nếu endDate ở quá khứ.
     */
    private boolean determineActiveStatus(LocalDate startDate, LocalDate endDate, boolean formIsActive) {
        if (!formIsActive) return false;

        // Lấy ngày hiện tại theo giờ VN
        LocalDate today = LocalDate.now(VIETNAM_ZONE);

        if (endDate.isBefore(today)) {
            throw new BusinessRuleException("Lỗi: Không thể kích hoạt coupon đã hết hạn (Ngày kết thúc < Hôm nay).");
        }

        // BỎ đoạn check startDate.isAfter(today) -> Cho phép admin lên lịch chạy ngầm.

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

    /**
     * SỬA LẠI LOGIC VALIDATE KHI KHÁCH ÁP DỤNG (QUAN TRỌNG NHẤT)
     * - Check song song: Cờ Active VÀ Thời gian thực.
     */
    @Override
    @Transactional(readOnly = true)
    public Coupon validateCoupon(String code, BigDecimal subtotal) {
        if (code == null || code.trim().isEmpty()) return null;

        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không hợp lệ"));

        // 1. Admin đã tắt nóng coupon này?
        if (!coupon.isActive()) {
            throw new BusinessRuleException("Mã giảm giá đã bị vô hiệu hóa hoặc chưa đến đợt chạy.");
        }

        // 2. CHECK THỜI GIAN THỰC (Phòng hờ scheduler chưa chạy hoặc bị treo)
        LocalDate today = LocalDate.now(VIETNAM_ZONE);

        if (today.isBefore(coupon.getStartDate())) {
            throw new BusinessRuleException("Mã giảm giá chưa có hiệu lực. Bắt đầu từ ngày: " + coupon.getStartDate());
        }

        if (today.isAfter(coupon.getEndDate())) {
            throw new BusinessRuleException("Mã giảm giá đã hết hạn sử dụng.");
        }

        // 3. Check số lượng
        if (coupon.getUsageLimit() > 0 && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BusinessRuleException("Mã giảm giá đã hết lượt sử dụng.");
        }

        // 4. Check giá trị đơn hàng
        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            // Format tiền cho đẹp (Tùy chọn)
            throw new BusinessRuleException(String.format("Đơn hàng chưa đạt giá trị tối thiểu (%,.0fđ) để áp dụng mã.", coupon.getMinOrderAmount()));
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
    public List<CouponResponseDTO> getAllPublicActiveCoupons() { // Hoặc hàm getPublicActiveCoupons tùy code bạn

        // Lấy ngày hiện tại theo giờ VN
        LocalDate today = LocalDate.now(VIETNAM_ZONE);

        // Gọi hàm query mới -> Chỉ lấy những cái ĐANG chạy thực sự
        List<Coupon> coupons = couponRepository.findValidCoupons(today);

        return coupons.stream()
                .map(this::mapToCouponDTO)
                .collect(Collectors.toList());
    }
}