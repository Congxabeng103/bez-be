package com.poly.bezbe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "product_options")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String name; // Ví dụ: "Màu sắc", "Kích cỡ"

    // Thêm cột này để giữ đúng thứ tự (Màu sắc luôn ở 1, Kích cỡ ở 2)
    @Column(name = "position", nullable = false)
    private Integer position;
    // --- THÊM TRƯỜNG CÒN THIẾU VÀO ĐÂY ---
    @OneToMany(mappedBy = "option", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC") // Luôn lấy value theo thứ tự
    private List<ProductOptionValue> values;
    // --- KẾT THÚC THÊM ---
}