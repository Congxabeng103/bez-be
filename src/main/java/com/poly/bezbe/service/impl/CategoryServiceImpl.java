package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.CategoryRequestDTO;
import com.poly.bezbe.dto.response.CategoryResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.entity.Category;
import com.poly.bezbe.entity.Product;
import com.poly.bezbe.exception.DuplicateResourceException; // <-- THÊM IMPORT
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.CategoryRepository;
import com.poly.bezbe.repository.ProductRepository;
import com.poly.bezbe.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException; // (Giữ lại vì bạn dùng ở permanentDelete)
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // (Hàm này đã chuẩn, có productCount)
    private CategoryResponseDTO mapToCategoryDTO(Category category) {
        long productCount = productRepository.countByCategoryId(category.getId());

        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.isActive())
                .imageUrl(category.getImageUrl())
                .productCount(productCount)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<CategoryResponseDTO> getAllCategories(Pageable pageable, String searchTerm, String status) {
        // (Logic này đã ổn)
        Page<Category> categoryPage;
        boolean searching = searchTerm != null && !searchTerm.isBlank();
        boolean activeFilter = !"INACTIVE".equalsIgnoreCase(status);

        if ("ALL".equalsIgnoreCase(status)) {
            if (searching) {
                categoryPage = categoryRepository.findByNameContainingIgnoreCase(searchTerm.trim(), pageable);
            } else {
                categoryPage = categoryRepository.findAll(pageable);
            }
        } else {
            if (searching) {
                categoryPage = categoryRepository.findByNameContainingIgnoreCaseAndActive(searchTerm.trim(), activeFilter, pageable);
            } else {
                categoryPage = categoryRepository.findAllByActive(activeFilter, pageable);
            }
        }

        List<CategoryResponseDTO> dtos = categoryPage.getContent().stream()
                .map(this::mapToCategoryDTO).collect(Collectors.toList());
        return new PageResponseDTO<>(dtos, categoryPage.getNumber(), categoryPage.getSize(),
                categoryPage.getTotalElements(), categoryPage.getTotalPages());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllCategoriesBrief() {
        return categoryRepository.findAllByActiveTrue(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(this::mapToCategoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO request) {
        String name = request.getName().trim();

        // === THÊM KIỂM TRA "CHECK FIRST" ===
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Tên danh mục '" + name + "' đã tồn tại.");
        }
        // === KẾT THÚC THÊM ===

        Category category = Category.builder()
                .name(name)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .active(request.isActive())
                .build();
        Category saved = categoryRepository.save(category);
        return mapToCategoryDTO(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Danh mục: " + id));

        String name = request.getName().trim();

        // === THÊM KIỂM TRA "CHECK FIRST" ===
        // "Nếu tên đang thay đổi VÀ tên mới trùng với của người khác"
        if (!category.getName().equalsIgnoreCase(name) &&
                categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {

            throw new DuplicateResourceException("Tên danh mục '" + name + "' đã được sử dụng.");
        }
        // === KẾT THÚC THÊM ===

        category.setName(name);
        category.setDescription(request.getDescription());
        category.setActive(request.isActive());
        category.setImageUrl(request.getImageUrl());

        Category updated = categoryRepository.save(category);
        return mapToCategoryDTO(updated);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        // (Logic này đã chuẩn)
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Danh mục: " + id));

        category.setActive(false);
        categoryRepository.save(category);

        List<Product> productsToHide = productRepository.findAllByCategoryIdAndActive(id, true);
        if (!productsToHide.isEmpty()) {
            for (Product product : productsToHide) {
                product.setActive(false);
            }
            productRepository.saveAll(productsToHide);
        }
    }

    @Override
    @Transactional
    public void permanentDeleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Danh mục: " + id));

        long productCount = productRepository.countByCategoryId(id);

        if (productCount > 0) {
            // === SỬA EXCEPTION (Để nhất quán với các Service khác) ===
            // Ném lỗi logic nghiệp vụ
            throw new IllegalStateException("Không thể xóa vĩnh viễn danh mục đang có " + productCount + " sản phẩm.");
            // =======================================================
        }

        categoryRepository.delete(category);
    }
}