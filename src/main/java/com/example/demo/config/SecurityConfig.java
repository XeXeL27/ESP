package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // ✅ Desactiva CSRF para permitir login manual
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/dashboard","/control/**", "/logout", "/logs", "/img/**").permitAll()
                .anyRequest().authenticated() // Resto requiere sesión
            )
            .formLogin(form -> form.disable()) // ❌ No usar login de Spring Security
            .logout(logout -> logout.disable()); // ❌ Logout lo controlas tú

        return http.build();
    }
}
