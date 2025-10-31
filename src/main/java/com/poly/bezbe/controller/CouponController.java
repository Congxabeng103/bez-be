package com.poly.bezbe.controller; // (Package của bạn)

import com.poly.bezbe.dto.request.CouponRequestDTO;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.CouponResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // SỬA HÀM NÀY (Thêm @RequestParam 'status')
    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<CouponResponseDTO>>> getAllCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "endDate,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status // <-- THÊM DÒNG NÀY
    ) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PageResponseDTO<CouponResponseDTO> couponPage = couponService.getAllCoupons(pageable, search, status);
        return ResponseEntity.ok(ApiResponseDTO.success(couponPage, "Lấy danh sách coupon thành công"));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<CouponResponseDTO>> createCoupon(
            @Valid @RequestBody CouponRequestDTO request) {
        CouponResponseDTO newCoupon = couponService.createCoupon(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newCoupon, "Tạo coupon thành công"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<CouponResponseDTO>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequestDTO request) {
        CouponResponseDTO updatedCoupon = couponService.updateCoupon(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedCoupon, "Cập nhật coupon thành công"));
    }

    // Sửa: Gọi Soft Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Object>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động coupon thành công"));
    }
}