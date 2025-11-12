package com.poly.bezbe.entity;

import com.poly.bezbe.enums.AuthProvider;
import com.poly.bezbe.enums.Gender;
// (Đã xóa import Position không dùng)
import com.poly.bezbe.enums.Role;
import jakarta.persistence.*;
// --- THÊM CÁC IMPORT VALIDATION ---
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
// ---
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set; // <-- 1. THÊM IMPORT Set
import java.util.stream.Collectors; // <-- 2. THÊM IMPORT Collectors

@Entity
@Table(name = "users")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Email không được để trống") // <-- Thêm
    @Email(message = "Email không đúng định dạng") // <-- Thêm
    @Size(max = 100, message = "Email không quá 100 ký tự") // <-- Thêm
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255) // (Password là nullable, rất đúng, vì dùng cho Social login)
    private String password;

    @Enumerated(EnumType.STRING) // Thêm lại dòng này
    @Column(nullable = false, length = 10) // Thêm lại dòng này
    private Role role; // <-- DÙNG LẠI DÒNG NÀY

    @NotNull // <-- Thêm
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuthProvider provider;

    @NotBlank(message = "Tên không được để trống") // <-- Thêm
    @Size(max = 50, message = "Tên không quá 50 ký tự") // <-- Thêm
    @Column(nullable = false, columnDefinition = "NVARCHAR(50)")
    private String firstName;

    @NotBlank(message = "Họ không được để trống") // <-- Thêm
    @Size(max = 50, message = "Họ không quá 50 ký tự") // <-- Thêm
    @Column(nullable = false, columnDefinition = "NVARCHAR(50)") // <-- Sửa: Thêm nullable=false
    private String lastName;

    @Column
    private String avatar;

    @Size(max = 15, message = "SĐT không quá 15 ký tự") // <-- Thêm
    @Column(length = 15, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Past(message = "Ngày sinh phải ở trong quá khứ") // <-- Thêm
    @Column
    private LocalDate dob; // Ngày sinh

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean isActive = false;

    @Column
    private String resetPasswordToken;

    @Column
    private String activationToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- SỬA LẠI HÀM GETAUTHORITIES ĐỂ DÙNG SET<ROLE> ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name())); // <-- DÙNG LẠI DÒNG NÀY
    }
    // --- KẾT THÚC SỬA ---

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

}