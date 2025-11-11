package com.poly.bezbe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "variant_option_values",
        uniqueConstraints = {
                // Đảm bảo 1 biến thể chỉ link tới 1 giá trị 1 lần
                @UniqueConstraint(columnNames = {"variant_id", "option_value_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantOptionValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private Variant variant;

    // Link tới giá trị của option (ví dụ: link tới "Đỏ")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_value_id", nullable = false)
    private ProductOptionValue optionValue;

    // Chúng ta cũng nên link trực tiếp tới Option (ví dụ: "Màu sắc")
    // để dễ dàng truy vấn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private ProductOption option;
}