package com.poly.bezbe.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

// --- Imports ---
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // <-- Thêm import này
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
// --- End Imports ---

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    // 1. TẠO BEAN CORS (Code của bạn đã có, giữ nguyên)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000")); // Cho phép React frontend
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Các phương thức cho phép
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type")); // Các header cho phép
        configuration.setAllowCredentials(true); // Cho phép gửi cookie/token
        configuration.setMaxAge(3600L); // Thời gian cache pre-flight request

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration); // Áp dụng cho tất cả API
        return source;
    }

    // 2. CẤU HÌNH SECURITY FILTER CHAIN (Đã sửa)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF (vì dùng JWT)
                .csrf(csrf -> csrf.disable())

                // --- SỬA 1: Áp dụng Bean CORS đã tạo ---
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(auth -> auth

                        // 1. API Công khai (Không cần đăng nhập)
                        .requestMatchers("/api/v1/auth/**").permitAll() // Đăng nhập, Đăng ký, Quên MK...

                        // 2. Phân quyền XEM (GET) cho Nhân viên (STAFF + ADMIN)
                        // Cho phép cả STAFF và ADMIN xem (GET) tất cả dữ liệu
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products/**",     // Xem sản phẩm
                                "/api/v1/categories/**", // Xem danh mục (kể cả /all-brief)
                                "/api/v1/brands/**",     // Xem thương hiệu (kể cả /all-brief)
                                "/api/v1/attributes/**", // Xem thuộc tính
                                "/api/v1/variants/**",   // Xem biến thể
                                "/api/v1/orders/**",     // Xem đơn hàng
                                "/api/v1/promotions/**", // Xem khuyến mãi (kể cả /brief) <-- SỬA LỖI CỦA BẠN
                                "/api/v1/coupons/**",    // Xem coupon
                                "/api/v1/users/customers", // Xem khách hàng
                                "/api/v1/users/employees"  // Xem nhân viên
                        ).hasAnyAuthority("ADMIN", "STAFF")

                        // 3. Phân quyền SỬA (PUT) cho Nhân viên (STAFF + ADMIN)
                        // (Ví dụ: Cho phép STAFF sửa đơn hàng)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/**").hasAnyAuthority("ADMIN", "STAFF")

                        // 4. Phân quyền Quản trị (ADMIN-only)
                        // Chỉ ADMIN mới được làm mọi thứ còn lại (POST, PUT, DELETE)
                        .requestMatchers("/api/v1/**").hasAuthority("ADMIN")

                        // 5. Mọi request còn lại (nếu có) phải đăng nhập
                        .anyRequest().authenticated()
                )
                // --- KẾT THÚC SỬA 2 ---

                // Cấu hình stateless (không dùng session)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}