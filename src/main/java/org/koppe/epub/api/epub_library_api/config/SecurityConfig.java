package org.koppe.epub.api.epub_library_api.config;

import org.koppe.epub.api.epub_library_api.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter filter;

    @Bean
    public SecurityFilterChain securityWebFilterChain(HttpSecurity http) {
        return http
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(CsrfConfigurer::disable).formLogin(FormLoginConfigurer::disable)
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(
                        exchange -> exchange
                                .requestMatchers("/auth/login", "/auth/refresh", "/v1/api-docs/**", "/v3/api-docs/**",
                                        "/swagger-ui/**", "/releases", "/releases/**")
                                .permitAll().anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
                }).accessDeniedHandler((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Forbidden\"}");
                })).build();
    }

    @PostConstruct
    public void setSecurityContextStrategy() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
}
