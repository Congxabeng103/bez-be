package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.CouponRequestDTO;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.CouponResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.entity.Coupon; // <-- 1. THÊM IMPORT NÀY
import com.poly.bezbe.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal; // <-- 2. THÊM IMPORT NÀY
import java.math.RoundingMode; // <-- 3. THÊM IMPORT NÀY

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // (Hàm này của bạn đã đúng)
    @GetMapping
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

    // (Hàm này của bạn đã đúng)
    @PostMapping
    public ResponseEntity<ApiResponseDTO<CouponResponseDTO>> createCoupon(
            @Valid @RequestBody CouponRequestDTO request) {
        CouponResponseDTO newCoupon = couponService.createCoupon(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newCoupon, "Tạo coupon thành công"), HttpStatus.CREATED);
    }

    // (Hàm này của bạn đã đúng)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<CouponResponseDTO>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequestDTO request) {
        CouponResponseDTO updatedCoupon = couponService.updateCoupon(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedCoupon, "Cập nhật coupon thành công"));
    }

    // (Hàm này của bạn đã đúng)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Object>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động coupon thành công"));
    }

    // --- 4. THÊM HÀM MỚI NÀY ĐỂ FRONTEND GỌI ---
    /**
     * API cho trang Checkout kiểm tra mã giảm giá
     * @param code Mã coupon (vd: "SALE100K")
     * @param subtotal Tạm tính của giỏ hàng (để kiểm tra minOrderAmount)
     */
    @GetMapping("/validate")
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