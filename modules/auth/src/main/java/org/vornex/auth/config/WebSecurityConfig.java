package org.vornex.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.vornex.auth.filter.JwtAuthenticationFilter;
import org.vornex.auth.service.AccessTokenService;
import org.vornex.authapi.AuthDetailsService;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final RestAuthenticationEntryPoint entryPoint;
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            AccessTokenService accessTokenService,
            AuthDetailsService authDetailsService,
            @Value("${security.auth.access-cookie-name:ACCESS_TOKEN}") String cookieName
    ) {
        // Лямбда для публичных эндпоинтов
        RequestMatcher publicEndpointsMatcher = request -> {
            String path = request.getServletPath();
            return path.equals("/auth/login") ||
                    path.equals("/auth/refresh") ||
                    path.startsWith("/auth/register") ||
                    path.startsWith("/public/") ||
                    path.equals("/") ||                  // главная
                    path.equals("/index.html") ||        // index.html
                    path.startsWith("/css/") ||         // стили
                    path.startsWith("/js/") ||          // скрипты
                    path.startsWith("/images/");        // картинки

        };

        return new JwtAuthenticationFilter(
                accessTokenService,
                authDetailsService,
                cookieName,         // имя куки
                true,                   // fallback через Authorization header
                publicEndpointsMatcher,  // передаем наш matcher
                entryPoint
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html",
                                "/css/**", "/js/**", "/images/**",
                                "/auth/login", "/auth/register", "/auth/refresh", "/public/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
    
