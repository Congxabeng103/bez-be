package com.poly.bezbe.service.impl; // <-- Chú ý package con 'impl'

import com.poly.bezbe.dto.request.CategoryRequestDTO;
import com.poly.bezbe.dto.response.CategoryResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.entity.Category;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.CategoryRepository;
import com.poly.bezbe.repository.ProductRepository;
import com.poly.bezbe.service.CategoryService; // <-- Import interface
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException; // <-- THÊM IMPORT
import java.util.List;
import java.util.stream.Collectors;
import com.poly.bezbe.entity.Product; // <-- THÊM IMPORT
@Service // <-- @Service được đặt ở đây
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService { // <-- Implement interface

    private final ProductRepository productRepository; // <-- THÊM DEPENDENCY
    private final CategoryRepository categoryRepository; // <-- THÊM DEPENDENCY

    // --- SỬA HÀM NÀY ---
    private CategoryResponseDTO mapToCategoryDTO(Category category) {
        // Đếm số sản phẩm liên quan
        long productCount = productRepository.countByCategoryId(category.getId());

        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.isActive())
                .imageUrl(category.getImageUrl())
                .productCount(productCount) // <-- Gán giá trị đếm được
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<CategoryResponseDTO> getAllCategories(Pageable pageable, String searchTerm, String status) {
        Page<Category> categoryPage;
        boolean searching = searchTerm != null && !searchTerm.isBlank();
        boolean activeFilter = !"INACTIVE".equalsIgnoreCase(status);

        if ("ALL".equalsIgnoreCase(status)) {
            // Logic ALL
            if (searching) {
                categoryPage = categoryRepository.findByNameContainingIgnoreCase(searchTerm.trim(), pageable);
            } else {
                categoryPage = categoryRepository.findAll(pageable);
            }
        } else {
            // Logic ACTIVE/INACTIVE
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
    @Override // <-- Thêm @Override
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
    @Override // <-- Thêm @Override
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO request) {
        // (Cân nhắc thêm kiểm tra trùng tên ở đây)
        Category category = Category.builder()
                .name(request.getName().trim())
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
    @Override // <-- Thêm @Override
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Danh mục: " + id));
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setActive(request.isActive());
        category.setImageUrl(request.getImageUrl()); // <-- THÊM DÒNG NÀY
        Category updated = categoryRepository.save(category);
        return mapToCategoryDTO(updated);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Danh mục: " + id));

        // 1. Ẩn danh mục
        category.setActive(false);
        categoryRepository.save(category);

        // 2. Ẩn hàng loạt các sản phẩm đang active thuộc danh mục này
        List<Product> productsToHide = productRepository.findAllByCategoryIdAndActive(id, true);
        if (!productsToHide.isEmpty()) {
            for (Product product : productsToHide) {
                product.setActive(false);
            }
            productRepository.saveAll(productsToHide);
        }
    }

    // --- THÊM HÀM MỚI (XÓA VĨNH VIỄN) ---
    @Override
    @Transactional
    public void permanentDeleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Danh mục: " + id));

        // 1. Kiểm tra xem có sản phẩm nào không
        long productCount = productRepository.countByCategoryId(id);

        if (productCount > 0) {
            // 2. Nếu có, ném lỗi, không cho xóa
            throw new DataIntegrityViolationException("Không thể xóa vĩnh viễn danh mục đang có sản phẩm. Chỉ có thể xóa khi không còn sản phẩm nào.");
        }

        // 3. Nếu không có, tiến hành xóa vĩnh viễn
        categoryRepository.delete(category);
    }
}