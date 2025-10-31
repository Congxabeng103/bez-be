package com.poly.bezbe.config;

// --- Imports (Giữ nguyên) ---
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // (Quan trọng)
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

    // (Bean CorsConfigurationSource của bạn giữ nguyên)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    // --- SỬA TOÀN BỘ HÀM NÀY (Sắp xếp lại thứ tự) ---
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth

                        // 1. API ADMIN (Yêu cầu ADMIN)
                        // (Chỉ ADMIN mới được POST, PUT, DELETE)
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/products/**", "/api/v1/categories/**", "/api/v1/brands/**",
                                "/api/v1/attributes/**", "/api/v1/variants/**", "/api/v1/promotions/**",
                                "/api/v1/coupons/**", "/api/v1/users/**"
                        ).hasAuthority("ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/products/**", "/api/v1/categories/**", "/api/v1/brands/**",
                                "/api/v1/attributes/**", "/api/v1/variants/**", "/api/v1/promotions/**",
                                "/api/v1/coupons/**", "/api/v1/users/**"
                        ).hasAuthority("ADMIN")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/products/**", "/api/v1/categories/**", "/api/v1/brands/**",
                                "/api/v1/attributes/**", "/api/v1/variants/**", "/api/v1/promotions/**",
                                "/api/v1/coupons/**", "/api/v1/users/**"
                        ).hasAuthority("ADMIN")

                        // 2. API NHÂN VIÊN (STAFF + ADMIN)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/orders/**",        // Xem đơn hàng
                                "/api/v1/users/customers",  // Xem khách hàng
                                "/api/v1/users/employees"   // Xem nhân viên
                        ).hasAnyAuthority("ADMIN", "STAFF")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/orders/**" // Cập nhật đơn hàng
                        ).hasAnyAuthority("ADMIN", "STAFF")

                        // (API GET riêng của Admin)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/attributes/**", "/api/v1/promotions/**", "/api/v1/coupons/**"
                        ).hasAuthority("ADMIN")


                        // 3. API CÔNG KHAI (PUBLIC - Cho phép tất cả)
                        .requestMatchers("/api/v1/auth/**").permitAll() // Đăng nhập, Đăng ký...

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products",          // Lấy list sản phẩm (Trang Products)
                                "/api/v1/products/detail/**",// Lấy chi tiết sản phẩm (Trang Detail)
                                "/api/v1/categories/all-brief", // Lấy list category cho filter
                                "/api/v1/brands/all-brief",     // Lấy list brand cho filter
                                "/api/v1/variants/find"      // Lấy variant khi user chọn
                        ).permitAll()
                        // --- KẾT THÚC SỬA ---

                        // 4. Mọi request còn lại (vd: /profile) phải đăng nhập
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}