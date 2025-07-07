package org.vornex.app.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class MDCFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MDCFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // Генерируем уникальный requestId для каждого запроса
            String requestId = UUID.randomUUID().toString();

            // Пытаемся получить имя пользователя из SecurityContext
            String username = "anonymous";
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
                    username = auth.getName();
                }
            } catch (Exception e) {
                log.debug("Failed to get username from SecurityContext", e);
            }

            // Кладём в MDC для текущего потока
            MDC.put("requestId", requestId);
            MDC.put("username", username);

            // Также можно добавить в response заголовок для отладки (по желанию)
            response.setHeader("X-Request-ID", requestId);

            filterChain.doFilter(request, response);
        } finally {
            // Обязательно чистим MDC чтобы не было утечек данных между потоками
            MDC.remove("requestId");
            MDC.remove("username");
        }
    }
}
