package com.poly.bezbe.entity;


import com.poly.bezbe.enums.AuthProvider;
import com.poly.bezbe.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

/**
 * Lớp User này là "bản thiết kế" cho đối tượng người dùng,
 * ánh xạ trực tiếp tới bảng `users` trong CSDL.
 * Nó cũng implement UserDetails để tích hợp với Spring Security.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Thông tin xác thực ---
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(length = 255) // Mật khẩu có thể null đối với tài khoản social
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuthProvider provider;

    // --- Thông tin cá nhân ---
    // ✨ Quan trọng: Dùng NVARCHAR để hỗ trợ tiếng Việt có dấu
    @Column(columnDefinition = "NVARCHAR(50)")
    private String firstName;

    @Column(columnDefinition = "NVARCHAR(50)")
    private String lastName;

    // --- Trạng thái và Token ---
    @Builder.Default // ✨ Đảm bảo giá trị mặc định được áp dụng khi dùng Builder
    private boolean isActive = false; // Mặc định là false để yêu cầu kích hoạt

    private String resetPasswordToken;

    @Column(name = "activation_token")
    private String activationToken;


    // --- Các phương thức bắt buộc của UserDetails ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Trạng thái tài khoản được quyết định bởi trường isActive
        return this.isActive;
    }
}