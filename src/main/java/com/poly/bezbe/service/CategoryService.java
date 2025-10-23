package com.poly.bezbe.service;

import com.poly.bezbe.dto.response.product.CategoryResponseDTO;
import com.poly.bezbe.entity.Category;
import com.poly.bezbe.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private CategoryResponseDTO mapToCategoryDTO(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                // Map các trường khác nếu có
                .build();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToCategoryDTO)
                .collect(Collectors.toList());
    }

    // (Bạn có thể thêm các hàm create, update, delete Category ở đây nếu cần)
}