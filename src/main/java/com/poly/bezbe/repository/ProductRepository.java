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

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // --- SỬA HÀM NÀY ---
    // Xóa hàm findBySearchAndStatus cũ và thay bằng hàm này:
    @EntityGraph(attributePaths = {"promotion", "category", "brand"})
    @Query("SELECT p FROM Product p LEFT JOIN p.category c LEFT JOIN p.brand b " + // Dùng LEFT JOIN
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
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchAndFilterProducts(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("activeStatus") boolean activeStatus, // Biến phụ trợ cho status
            @Param("categoryName") String categoryName,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable
    );
    // --- KẾT THÚC SỬA ĐỔI ---

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
}