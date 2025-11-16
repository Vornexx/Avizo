package org.vornex.auth.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.vornex.auth.util.RateLimiterWrapper;

import java.time.Duration;
import java.util.Map;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

//    private final RateLimiterWrapper rateLimiter;
//
//    // Настраиваем лимиты для эндпоинтов: capacity, refillAmount, refillDuration
//    private final Map<String, EndpointLimit> endpointLimits = Map.of(
//            "/auth/login", new EndpointLimit(5, 1, Duration.ofMinutes(1)),
//            "/auth/register", new EndpointLimit(3, 1, Duration.ofHours(1))
//    );
//
//    public RateLimitInterceptor(RateLimiterWrapper rateLimiter) {
//        this.rateLimiter = rateLimiter;
//    }
//
//    @Override
//    public boolean preHandle(HttpServletRequest request,
//                             @NonNull HttpServletResponse response,
//                             @NonNull Object handler) throws Exception {
//        String path = request.getRequestURI();
//        EndpointLimit limit = endpointLimits.get(path);
//
//        if (limit != null) {
//            String key = buildKey(request);
//
//            boolean allowed = rateLimiter.tryConsume(
//                    key,
//                    limit.capacity,
//                    limit.refillAmount,
//                    limit.duration
//            );
//
//            if (!allowed) {
//                response.setStatus(429); // Too Many Requests
//                response.getWriter().write("Too many requests. Please try again later.");
//                return false;
//            }
//        }
//        return true;
//    }
//
//    private String buildKey(HttpServletRequest request) {
//        String ip = request.getHeader("X-Forwarded-For");
//        if (ip == null || ip.isBlank()) {
//            ip = request.getRemoteAddr();
//        } else {
//            ip = ip.split(",")[0].trim();
//        }
//        return ip;
//    }
//
//    private record EndpointLimit(long capacity, long refillAmount, Duration duration) {
//    }
}
