package com.poly.bezbe.controller;


import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.product.BrandResponseDTO;
import com.poly.bezbe.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands") // Base path cho Brand
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    // API để lấy tất cả thương hiệu (cho dropdown)
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<BrandResponseDTO>>> getAllBrands() {
        List<BrandResponseDTO> brands = brandService.getAllBrands();
        return ResponseEntity.ok(ApiResponseDTO.success(brands, "Lấy danh sách thương hiệu thành công"));
    }

}