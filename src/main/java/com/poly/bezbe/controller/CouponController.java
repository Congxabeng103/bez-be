package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.CouponRequestDTO;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.CouponResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.entity.Coupon;
import com.poly.bezbe.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // <-- IMPORT
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // Lấy danh sách cho trang Admin (SỬA: Thêm 'STAFF')
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')") // <-- ĐÃ THÊM 'STAFF'
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<CouponResponseDTO>>> getAllCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "endDate,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status
    ) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PageResponseDTO<CouponResponseDTO> couponPage = couponService.getAllCoupons(pageable, search, status);
        return ResponseEntity.ok(ApiResponseDTO.success(couponPage, "Lấy danh sách coupon thành công"));
    }

    // Tạo mới (Chỉ Manager / Admin)
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponseDTO<CouponResponseDTO>> createCoupon(
            @Valid @RequestBody CouponRequestDTO request) {
        CouponResponseDTO newCoupon = couponService.createCoupon(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newCoupon, "Tạo coupon thành công"), HttpStatus.CREATED);
    }

    // Cập nhật (Chỉ Manager / Admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponseDTO<CouponResponseDTO>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequestDTO request) {
        CouponResponseDTO updatedCoupon = couponService.updateCoupon(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedCoupon, "Cập nhật coupon thành công"));
    }

    // Ngừng hoạt động (Chỉ Manager / Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponseDTO<Object>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động coupon thành công"));
    }

    // API Kiểm tra mã (Dành cho User đã đăng nhập)
    @GetMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<BigDecimal>> validateCouponForCheckout(
            @RequestParam String code,
            @RequestParam BigDecimal subtotal) {

        // 1. Gọi service (Hàm này sẽ ném Exception nếu 404, 400)
        Coupon coupon = couponService.validateCoupon(code, subtotal);

        // 2. Tính toán số tiền giảm (Logic giống hệt OrderService)
        BigDecimal couponDiscount = BigDecimal.ZERO;

        // (Vì logic của bạn chỉ có %, ta làm như sau:)
        couponDiscount = subtotal.multiply(coupon.getDiscountValue())
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

        // 3. Kiểm tra giảm giá tối đa
        if (coupon.getMaxDiscountAmount() != null && couponDiscount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            couponDiscount = coupon.getMaxDiscountAmount();
        }

        // 4. Trả về số tiền giảm được
        return ResponseEntity.ok(ApiResponseDTO.success(couponDiscount, "Áp dụng mã thành công"));
    }
}