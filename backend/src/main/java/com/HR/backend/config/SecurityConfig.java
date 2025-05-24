package com.HR.backend.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOrigins(List.of("http://localhost:4200"));
                    corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(List.of("*"));
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/departments/**").hasAuthority("HR_MANAGER")
                        .requestMatchers("/api/employees/**").hasAuthority("HR_MANAGER")
                        .requestMatchers("/api/hr/**").hasAuthority("HR_MANAGER")
                        .requestMatchers("/api/employee/**").hasAuthority("HR_MANAGER")
                        .requestMatchers("/api/profile").authenticated()
                        .requestMatchers("/api/leave/submit", "/api/leave/my-requests", "/api/leave/cancel/**","/api/leave/balance").authenticated()
                        .requestMatchers("/api/leave/pending", "/api/leave/review/**").hasAuthority("HR_MANAGER")
                        .requestMatchers("/api/training/all", "/api/training/my-enrollments", "/api/training/enroll/**", "/api/training/cancel/**").authenticated()
                        .requestMatchers("/api/training/create", "/api/training/update/**", "/api/training/delete/**", "/api/training/review/**", "/api/training/pending-enrollments").hasAuthority("HR_MANAGER")
                        .requestMatchers("/api/documents/generate", "/api/documents/upload").hasAuthority("HR_MANAGER")
                        .requestMatchers("/api/documents/my-requests").authenticated()
                        .requestMatchers("/api/documents/pending-requests", "/api/documents/review-request/**").hasAuthority("HR_MANAGER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.getWriter().write("{\"message\": \"Unauthorized\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.getWriter().write("{\"message\": \"You do not have permission to perform this action\"}");
                        }));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}