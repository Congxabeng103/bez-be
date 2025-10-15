package com.poly.bezbe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_variant_values",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"variant_id", "attribute_value_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_value_id", nullable = false)
    private ProductVariantAttributeValue attributeValue;
}