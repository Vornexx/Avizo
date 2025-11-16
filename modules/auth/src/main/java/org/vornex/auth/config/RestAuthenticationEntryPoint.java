package org.vornex.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Log4j2
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // логируем реальный cause
        if (authException.getCause() != null) {
            log.warn("Authentication failed at {}: {}", request.getRequestURI(), authException.getCause().getMessage(), authException.getCause());
        } else {
            log.warn("Authentication failed at {}: {}", request.getRequestURI(), authException.getMessage(), authException);
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json");

        // Безопасная причина для клиента
        String message = authException.getMessage() != null
                ? authException.getMessage()
                : "Unauthorized";

        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
