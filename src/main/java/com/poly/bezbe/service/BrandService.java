package com.poly.bezbe.service;

import com.poly.bezbe.dto.response.product.BrandResponseDTO;
import com.poly.bezbe.entity.Brand;
import com.poly.bezbe.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    // Hàm chuyển đổi entity sang DTO
    private BrandResponseDTO mapToBrandDTO(Brand brand) {
        return BrandResponseDTO.builder()
                .id(brand.getId())
                .name(brand.getName())
                // Map các trường khác nếu có
                .build();
    }

    // Lấy tất cả thương hiệu
    @Transactional(readOnly = true)
    public List<BrandResponseDTO> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(this::mapToBrandDTO)
                .collect(Collectors.toList());
    }

    // (Bạn có thể thêm các hàm create, update, delete Brand nếu cần)
}