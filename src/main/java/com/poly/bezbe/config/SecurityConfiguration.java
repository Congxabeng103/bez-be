package com.poly.bezbe.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Kích hoạt @PreAuthorize ở Controller
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://white-flower-07d68e500.3.azurestaticapps.net"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth

                        // 1. API CÔNG KHAI
                        .requestMatchers("/api/v1/auth/**", "/api/v1/contact").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products/**", // Gom gọn lại
                                "/api/v1/categories/all-brief",
                                "/api/v1/brands/all-brief",
                                "/api/v1/variants/find",
                                "/api/v1/reviews/**",
                                "/api/v1/payment/vnpay-return/**",
                                "/api/v1/payment/vnpay-ipn/**",
                                "/api/v1/promotions/public/**",
                                "/api/v1/coupons/public/**"
                        ).permitAll()

                        // 2. API CỦA USER (ĐÃ ĐĂNG NHẬP) - Profile & Order cá nhân
                        .requestMatchers(
                                "/api/v1/users/profile/**", // Bao gồm cả update address, password
                                "/api/v1/users/update-password",
                                "/api/v1/orders/my-orders/**",
                                "/api/v1/orders/create",
                                "/api/v1/coupons/validate",
                                "/api/v1/payment/{orderId}/retry-vnpay"
                        ).authenticated()

                        // 3. API VẬN HÀNH (STAFF, MANAGER, ADMIN)
                        .requestMatchers(
                                "/api/v1/orders/**",
                                "/api/v1/payment/**"
                        ).hasAnyAuthority("ADMIN", "STAFF", "MANAGER")

                        // 4. API QUẢN TRỊ (MANAGER, ADMIN) - Sản phẩm, Khuyến mãi
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/products/**", "/api/v1/categories/**", "/api/v1/brands/**",
                                "/api/v1/attributes/**", "/api/v1/variants/**",
                                "/api/v1/promotions/**", "/api/v1/coupons/**"
                        ).hasAnyAuthority("ADMIN", "MANAGER")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/products/**", // Đã xóa chữ 'j' thừa
                                "/api/v1/categories/**", "/api/v1/brands/**",
                                "/api/v1/attributes/**", "/api/v1/variants/**",
                                "/api/v1/promotions/**", "/api/v1/coupons/**"
                        ).hasAnyAuthority("ADMIN", "MANAGER")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/products/**", "/api/v1/categories/**", "/api/v1/brands/**",
                                "/api/v1/attributes/**", "/api/v1/variants/**",
                                "/api/v1/promotions/**", "/api/v1/coupons/**"
                        ).hasAnyAuthority("ADMIN", "MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/v1/dashboard/**").hasAnyAuthority("ADMIN", "MANAGER")

                        // ==================================================================
                        // ⭐ SỬA LỖI QUAN TRỌNG TẠI ĐÂY ⭐
                        // Thay vì chặn cứng bằng .hasAuthority("ADMIN"), ta mở cửa cho authenticated()
                        // Để Controller (@PreAuthorize) tự phân quyền ai được xóa, ai được sửa.
                        // ==================================================================
                        .requestMatchers("/api/v1/users/**").authenticated()

                        // 6. Mọi request còn lại
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