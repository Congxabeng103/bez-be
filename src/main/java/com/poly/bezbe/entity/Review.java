package com.poly.bezbe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    // --- SỬA LẠI CHỖ NÀY ---
    @Column(name = "visible", nullable = false, columnDefinition = "bit default 1")
    @Builder.Default // <--- QUAN TRỌNG: Để khi tạo mới mặc định là true
    private boolean visible = true;
    // -----------------------

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}