package com.poly.bezbe.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Variant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku", unique = true)
    private String sku;

    @Column(name = "name", columnDefinition = "NVARCHAR(255)")
    private String name;

    @Column(name = "price", precision = 19, scale = 2)
    private BigDecimal price;

    @Min(value = 0, message = "Số lượng tồn kho không thể âm") // <-- 2. Chặn ở lớp Java (Validation)
    // 3. Chặn cứng ở lớp Database (SQL Server/MySQL đều hiểu CHECK)
    @Column(name = "stock_quantity", columnDefinition = "int DEFAULT 0 CHECK (stock_quantity >= 0)")
    private Integer stockQuantity;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(512)") // <-- SỬA
    private String imageUrl;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 1")
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- BẰNG DÒNG NÀY ---
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VariantOptionValue> optionValues; // <-- DÒNG MỚI
}