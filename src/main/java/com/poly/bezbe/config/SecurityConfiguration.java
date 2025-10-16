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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        // Khu vực công khai: ai cũng vào được (bao gồm cả callback của OAuth2)
                        .requestMatchers("/api/v1/auth/**", "/login/oauth2/**").permitAll()
                        // ✨ QUY TẮC BẢO VỆ: Yêu cầu quyền ADMIN cho tất cả API trong khu vực /admin/**
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ADMIN")
                        // Mọi khu vực khác: phải đăng nhập
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
                // ✨ Kích hoạt luồng đăng nhập OAuth2 với các cấu hình mặc định.
                // Spring sẽ tự động tạo endpoint /oauth2/authorization/google
                //.oauth2Login(Customizer.withDefaults());

        return http.build();
    }
}