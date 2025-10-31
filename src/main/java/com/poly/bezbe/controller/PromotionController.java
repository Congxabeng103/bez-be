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
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    // SỬA HÀM NÀY (Thêm @RequestParam 'status')
    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<PromotionResponseDTO>>> getAllPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status // <-- THÊM DÒNG NÀY
    ) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PageResponseDTO<PromotionResponseDTO> promotionPage = promotionService.getAllPromotions(pageable, search, status);
        return ResponseEntity.ok(ApiResponseDTO.success(promotionPage, "Lấy danh sách khuyến mãi thành công"));
    }

    // Lấy danh sách rút gọn (cho Product form)
    @GetMapping("/brief")
    public ResponseEntity<ApiResponseDTO<List<PromotionBriefDTO>>> getPromotionBriefList() {
        List<PromotionBriefDTO> promotions = promotionService.getPromotionBriefList();
        return ResponseEntity.ok(ApiResponseDTO.success(promotions, "Lấy danh sách tóm tắt khuyến mãi thành công"));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<PromotionResponseDTO>> createPromotion(
            @Valid @RequestBody PromotionRequestDTO request) {
        PromotionResponseDTO newPromotion = promotionService.createPromotion(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newPromotion, "Tạo khuyến mãi thành công"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<PromotionResponseDTO>> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionRequestDTO request) {
        PromotionResponseDTO updatedPromotion = promotionService.updatePromotion(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedPromotion, "Cập nhật khuyến mãi thành công"));
    }

    // Sửa: Gọi Soft Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Object>> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động khuyến mãi thành công"));
    }
}