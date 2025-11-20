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
    // Thêm LEFT JOIN p.promotion prom
    @Query("SELECT p FROM Product p LEFT JOIN p.category c LEFT JOIN p.brand b LEFT JOIN p.promotion prom " +
            "WHERE " +
            // 1. Lọc Status
            "(:status = 'ALL' OR p.active = :activeStatus) " +

            // 2. Lọc Search
            "AND (:searchTerm IS NULL OR LOWER(p.name) LIKE LOWER(concat('%', :searchTerm, '%'))) " +

            // 3. Lọc Category Name
            "AND (:categoryName IS NULL OR c.name = :categoryName) " +

            // 4. Lọc Brand Name (MỚI THÊM)
            "AND (:brandName IS NULL OR b.name = :brandName) " +

            // 5. Lọc Promotion ID (MỚI THÊM)
            "AND (:promotionId IS NULL OR prom.id = :promotionId) " +

            // 6. Lọc Min Price
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +

            // 7. Lọc Max Price
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +

            // 8. Lọc Biến thể
            "AND (:hasVariants IS NULL OR :hasVariants = false OR EXISTS (SELECT v FROM Variant v WHERE v.product = p AND v.active = true))")
    Page<Product> searchAndFilterProducts(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("activeStatus") boolean activeStatus,
            @Param("categoryName") String categoryName,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("hasVariants") Boolean hasVariants,
            // --- THÊM 2 PARAM NÀY ---
            @Param("brandName") String brandName,
            @Param("promotionId") Long promotionId,
            // ------------------------
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