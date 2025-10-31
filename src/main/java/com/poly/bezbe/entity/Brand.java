package com.poly.bezbe.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "brands")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, columnDefinition = "NVARCHAR(255)")
    private String name;

    @Column(name = "description", columnDefinition = "NVARCHAR(1000)")
    private String description;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(512)") // <-- SỬA
    private String imageUrl;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "brand")
    private List<Product> products;
}