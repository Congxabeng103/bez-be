package com.poly.bezbe.controller;

// --- Imports ---
import com.poly.bezbe.dto.request.PromotionRequestDTO; // DTO không có productIds
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.PromotionBriefDTO; // DTO rút gọn (ID, Name)
import com.poly.bezbe.dto.response.PromotionResponseDTO; // DTO không có productIds
import com.poly.bezbe.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List; // Import List
// --- End Imports ---

@RestController
@RequestMapping("/api/v1/promotions") // Đường dẫn gốc cho Promotion
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    /**
     * API lấy danh sách Khuyến mãi (%) (có phân trang, tìm kiếm, sắp xếp).
     * GET /api/v1/promotions?page=0&size=5&search=hè&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<PromotionResponseDTO>>> getAllPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search, // Tìm theo tên khuyến mãi
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PageResponseDTO<PromotionResponseDTO> promotionPage = promotionService.getAllPromotions(pageable, search);
        return ResponseEntity.ok(ApiResponseDTO.success(promotionPage, "Lấy danh sách khuyến mãi thành công"));
    }

    /**
     * API tạo Khuyến mãi (%) mới.
     * POST /api/v1/promotions
     * Body: PromotionRequestDTO (không có productIds)
     */
    @PostMapping
    public ResponseEntity<ApiResponseDTO<PromotionResponseDTO>> createPromotion(
            @Valid @RequestBody PromotionRequestDTO request) {
        PromotionResponseDTO newPromotion = promotionService.createPromotion(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newPromotion, "Tạo khuyến mãi thành công"), HttpStatus.CREATED);
    }

    /**
     * API cập nhật Khuyến mãi (%).
     * PUT /api/v1/promotions/{id}
     * Body: PromotionRequestDTO (không có productIds)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<PromotionResponseDTO>> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionRequestDTO request) {
        PromotionResponseDTO updatedPromotion = promotionService.updatePromotion(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedPromotion, "Cập nhật khuyến mãi thành công"));
    }

    /**
     * API xóa Khuyến mãi (%).
     * DELETE /api/v1/promotions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Object>> deletePromotion(
            @PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Xóa khuyến mãi thành công"));
    }

    /**
     * API lấy danh sách Khuyến mãi (%) rút gọn (ID, Name) cho dropdown.
     * GET /api/v1/promotions/brief
     * Được gọi bởi form trong ProductManagement.tsx
     */
    @GetMapping("/brief")
    public ResponseEntity<ApiResponseDTO<List<PromotionBriefDTO>>> getPromotionBriefList() {
        List<PromotionBriefDTO> promotions = promotionService.getPromotionBriefList();
        return ResponseEntity.ok(ApiResponseDTO.success(promotions, "Lấy danh sách tóm tắt khuyến mãi thành công"));
    }
}