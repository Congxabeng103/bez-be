package com.poly.bezbe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_option_values")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductOptionValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private ProductOption option;

    @Column(name = "value", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String value; // Ví dụ: "Đỏ", "S", "M"

    // Thêm cột này để giữ đúng thứ tự (S, M, L)
    @Column(name = "position", nullable = false)
    private Integer position;
}