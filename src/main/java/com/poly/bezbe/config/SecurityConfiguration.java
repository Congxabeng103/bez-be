package com.poly.bezbe.config;

// --- Imports (Giữ nguyên) ---
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth

                        // 1. API ADMIN (Giữ nguyên)
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

                        // 2. API CỦA USER (Giữ nguyên)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/coupons/validate",
                                "/api/v1/orders/my-orders",
                                "/api/v1/orders/my-orders/**"
                        ).authenticated()
                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/orders/my-orders/**"
                        ).authenticated()

                        // 3. API NHÂN VIÊN (Giữ nguyên)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/orders/**",
                                "/api/v1/users/customers",
                                "/api/v1/users/employees"
                        ).hasAnyAuthority("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/orders/**"
                        ).hasAnyAuthority("ADMIN", "STAFF")

                        // 4. API GET CÒN LẠI CỦA ADMIN (Giữ nguyên)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/attributes/**",
                                "/api/v1/promotions/**",
                                "/api/v1/coupons/**"
                        ).hasAuthority("ADMIN")


                        // 5. API CÔNG KHAI (PUBLIC - Cho phép tất cả)
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products",
                                "/api/v1/products/detail/**",
                                "/api/v1/categories/all-brief",
                                "/api/v1/brands/all-brief",
                                "/api/v1/variants/find" ,
                                "/api/v1/payment/vnpay-return/**",

                                // --- SỬA Ở ĐÂY ---
                                // Thêm dòng này để "mở cổng" cho IPN
                                "/api/v1/payment/vnpay-ipn/**"
                                // --- KẾT THÚC SỬA ---

                        ).permitAll()

                        // 6. Mọi request còn lại phải đăng nhập (Giữ nguyên)
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