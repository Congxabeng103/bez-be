package com.poly.bezbe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_variant_attributes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name; // Ví dụ: "Color", "Size"
}