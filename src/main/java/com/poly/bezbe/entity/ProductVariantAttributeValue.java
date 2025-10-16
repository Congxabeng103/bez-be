package com.poly.bezbe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_variant_attribute_values")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVariantAttributeValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private ProductVariantAttribute attribute;

    @Column(name = "value", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String value; // Ví dụ: "Đỏ", "XL"
}