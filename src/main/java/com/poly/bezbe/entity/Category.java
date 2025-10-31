package com.poly.bezbe.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;


import jakarta.persistence.*;
import lombok.*;
import java.util.List; // Import nếu bạn cần liên kết ngược lại

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, columnDefinition = "NVARCHAR(255)")
    private String name;

    @Column(name = "description", columnDefinition = "NVARCHAR(1000)")
    private String description; // (Tùy chọn) Thêm mô tả

    @Column(name = "image_url", length = 512) // Thêm cột này
    private String imageUrl;

    @Column(name = "active", nullable = false)
    private boolean active = true; // <-- THÊM TRƯỜNG NÀY (Mặc định là true)

    // (Tùy chọn: Liên kết ngược lại với Product)
     @OneToMany(mappedBy = "category")
     private List<Product> products;
}