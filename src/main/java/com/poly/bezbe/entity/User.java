package com.poly.bezbe.entity;

import com.poly.bezbe.enums.AuthProvider;
import com.poly.bezbe.enums.Gender;
import com.poly.bezbe.enums.Position; // <-- 1. Import Enum Position
import com.poly.bezbe.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDate;
import java.time.LocalDateTime; // <-- 2. Import LocalDateTime
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuthProvider provider;

    @Column(nullable = false, columnDefinition = "NVARCHAR(50)")
    private String firstName;

    @Column(columnDefinition = "NVARCHAR(50)")
    private String lastName;

    @Column
    private String avatar;

    // --- 3. CÁC TRƯỜNG ĐÃ CẬP NHẬT ---
    @Column(length = 15, unique = true) // SĐT có thể là unique
    private String phone;

    @Enumerated(EnumType.STRING) // Lưu tên Enum (vd: "QUAN_TRI_VIEN")
    @Column(columnDefinition = "NVARCHAR(100)")
    private Position position; // Đổi String thành Enum Position
    // --- KẾT THÚC CẬP NHẬT ---
// --- THÊM 2 TRƯỜNG NÀY ---
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column
    private LocalDate dob; // Ngày sinh
    // --- KẾT THÚC THÊM ---
    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0") // Thêm default 0
    private boolean isActive = false;

    @Column
    private String resetPasswordToken;

    @Column
    private String activationToken;

    // --- 4. NGÀY THAM GIA ---
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    // --- KẾT THÚC THÊM ---

    // (Giả sử bạn đã có @UpdateTimestamp cho 'updated_at' nếu cần)

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return this.isActive; }

    // (Các liên kết @OneToMany đến Order, Review... nếu có)
}