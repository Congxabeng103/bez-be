package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Product;
import com.poly.bezbe.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph; // Import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import
import org.springframework.data.repository.query.Param; // Import
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // --- SỬA HÀM NÀY ---
    @EntityGraph(attributePaths = {"promotion", "category", "brand"})
    @Query("SELECT p FROM Product p LEFT JOIN p.category c LEFT JOIN p.brand b " +
            "WHERE " +
            // 1. Lọc Status (active/inactive/all)
            "(:status = 'ALL' OR p.active = :activeStatus) " +

            // 2. Lọc Search
            "AND (:searchTerm IS NULL OR LOWER(p.name) LIKE LOWER(concat('%', :searchTerm, '%'))) " +

            // 3. Lọc Category Name
            "AND (:categoryName IS NULL OR c.name = :categoryName) " +

            // 4. Lọc Min Price
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +

            // 5. Lọc Max Price
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +

            // 6. === SỬA LOGIC LỌC BIẾN THỂ (Đã bỏ cặp ngoặc thừa) ===
            "AND (:hasVariants IS NULL OR :hasVariants = false OR EXISTS (SELECT v FROM Variant v WHERE v.product = p AND v.active = true))") // <-- ĐÃ SỬA DÒNG NÀY
    Page<Product> searchAndFilterProducts(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("activeStatus") boolean activeStatus, // Biến phụ trợ cho status
            @Param("categoryName") String categoryName,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("hasVariants") Boolean hasVariants, // Tham số đã có
            Pageable pageable
    );
    // --- KẾT THÚC SỬA ĐỔI ---

    // (Các hàm bên dưới giữ nguyên)

    // 2. Dùng cho getProductBriefList (Chỉ lấy Active, TÌM KIẾM)
    @EntityGraph(attributePaths = {"variants"})
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    // 3. Dùng cho getProductBriefList (Chỉ lấy Active, KHÔNG TÌM KIẾM)
    @EntityGraph(attributePaths = {"variants"})
    Page<Product> findAllByActiveTrue(Pageable pageable);

    // THÊM HÀM NÀY (Để đếm sản phẩm)
    long countByCategoryId(Long categoryId);

    // THÊM HÀM NÀY (Để ẩn hàng loạt)
    List<Product> findAllByCategoryIdAndActive(Long categoryId, boolean active);

    // (Lát nữa bạn cũng sẽ cần 2 hàm tương tự cho Brand)
    long countByBrandId(Long brandId);
    List<Product> findAllByBrandIdAndActive(Long brandId, boolean active);
    List<Product> findByCategoryIdAndIdNotAndActiveTrue(Long categoryId, Long productId, Pageable pageable);
    long countByPromotion(Promotion promotion);
    boolean existsByNameIgnoreCase(String name);

    // Dùng cho hàm updateProduct
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    @Query("SELECT MAX(p.price) FROM Product p WHERE p.active = true")
    BigDecimal findHighestActiveProductPrice();
}