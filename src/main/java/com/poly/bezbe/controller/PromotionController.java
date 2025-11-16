package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.PromotionRequestDTO;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.PromotionBriefDTO;
import com.poly.bezbe.dto.response.PromotionResponseDTO;
import com.poly.bezbe.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // <-- 1. IMPORT
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    // Lấy danh sách cho trang Admin (SỬA: Thêm 'STAFF')
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')") // <-- 2. ĐÃ THÊM 'STAFF'
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<PromotionResponseDTO>>> getAllPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status
    ) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PageResponseDTO<PromotionResponseDTO> promotionPage = promotionService.getAllPromotions(pageable, search, status);
        return ResponseEntity.ok(ApiResponseDTO.success(promotionPage, "Lấy danh sách khuyến mãi thành công"));
    }

    // Lấy danh sách rút gọn (Cả 3 vai trò - cho trang sản phẩm)
    @GetMapping("/brief")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')") // <-- 3. (Giữ nguyên, đã đúng)
    public ResponseEntity<ApiResponseDTO<List<PromotionBriefDTO>>> getPromotionBriefList() {
        List<PromotionBriefDTO> promotions = promotionService.getPromotionBriefList();
        return ResponseEntity.ok(ApiResponseDTO.success(promotions, "Lấy danh sách tóm tắt khuyến mãi thành công"));
    }

    // Tạo mới (Chỉ Manager / Admin)
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')") // <-- 4. (Giữ nguyên)
    public ResponseEntity<ApiResponseDTO<PromotionResponseDTO>> createPromotion(
            @Valid @RequestBody PromotionRequestDTO request) {
        PromotionResponseDTO newPromotion = promotionService.createPromotion(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newPromotion, "Tạo khuyến mãi thành công"), HttpStatus.CREATED);
    }

    // Cập nhật (Chỉ Manager / Admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')") // <-- 5. (Giữ nguyên)
    public ResponseEntity<ApiResponseDTO<PromotionResponseDTO>> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionRequestDTO request) {
        PromotionResponseDTO updatedPromotion = promotionService.updatePromotion(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedPromotion, "Cập nhật khuyến mãi thành công"));
    }

    // Ngừng hoạt động (Chỉ Manager / Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')") // <-- 6. (Giữ nguyên)
    public ResponseEntity<ApiResponseDTO<Object>> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động khuyến mãi thành công"));
    }
    @DeleteMapping("/permanent-delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')") // Chỉ Admin/Manager
    public ResponseEntity<ApiResponseDTO<Object>> permanentDeletePromotion(@PathVariable Long id) {
        // Hàm này bạn sẽ cần tạo ở Service
        promotionService.permanentDeletePromotion(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Xóa vĩnh viễn khuyến mãi thành công"));
    }
    @GetMapping("/public/latest")
    @PreAuthorize("permitAll()") // <-- Cho phép tất cả mọi người
    public ResponseEntity<ApiResponseDTO<PromotionResponseDTO>> getLatestPublicPromotion() {

        // Gọi hàm service đơn giản (vì scheduler đã lo)
        PromotionResponseDTO latestPromotion = promotionService.getLatestActivePromotion();

        // Trả về 200 OK (dù data có là null hay không)
        return ResponseEntity.ok(ApiResponseDTO.success(latestPromotion,
                latestPromotion != null ? "Lấy khuyến mãi mới nhất thành công" : "Không có khuyến mãi nào đang hoạt động"));
    }
    /**
     * (MỚI) API PUBLIC: Lấy TẤT CẢ KM đang active
     * (Dùng cho banner carousel trang chủ)
     */
    @GetMapping("/public/active")
    @PreAuthorize("permitAll()") // <-- Cho phép tất cả
    public ResponseEntity<ApiResponseDTO<List<PromotionResponseDTO>>> getPublicActivePromotions() {

        List<PromotionResponseDTO> promotions = promotionService.getActivePromotions();

        return ResponseEntity.ok(ApiResponseDTO.success(promotions,
                "Lấy danh sách khuyến mãi active thành công"));
    }
}