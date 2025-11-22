package org.vornex.auth.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.vornex.auth.config.RestAuthenticationEntryPoint;
import org.vornex.auth.service.AccessTokenService;
import org.vornex.authapi.AuthDetailsService;
import org.vornex.authapi.AuthUserData;
import org.vornex.userapi.AccountStatus;
import org.vornex.userapi.PermissionDto;
import org.vornex.userapi.RoleDto;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

// валидация только access, если его нет (кука удаленна или просрочен токен) spring security выкидывает 401 и фронтенд делает запрос на refresh
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final AccessTokenService accessTokenService;
    private final AuthDetailsService authDetailsService;

    private final String accessCookieName;
    private final boolean allowAuthorizationHeaderFallback;
    private final RestAuthenticationEntryPoint entryPoint;

    public JwtAuthenticationFilter(AccessTokenService accessTokenService,
                                   AuthDetailsService authDetailsService,
                                   String accessCookieName,
                                   boolean allowAuthorizationHeaderFallback
                                   , RestAuthenticationEntryPoint entryPoint) {
        this.accessTokenService = requireNonNull(accessTokenService);
        this.authDetailsService = requireNonNull(authDetailsService);
        this.entryPoint = entryPoint;
        if (!StringUtils.hasText(accessCookieName)) {
            throw new IllegalArgumentException("accessCookieName must not be blank");
        }
        this.accessCookieName = accessCookieName;
        this.allowAuthorizationHeaderFallback = allowAuthorizationHeaderFallback;
    }


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Если аутентификация уже установлена, ничего не делаем
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current == null || !current.isAuthenticated()) {
            try {
                // Пытаемся аутентифицировать пользователя через JWT
                tryAuthenticate(request);
            } catch (AuthenticationException ex) {
                // Любая ошибка аутентификации делегируется RestAuthenticationEntryPoint
                // Это гарантирует корректный JSON ответ вместо дефолтного "Full authentication is required"
                entryPoint.commence(request, response, ex);
                return; // Прерываем цепочку фильтров, дальше запрос не идет
            } catch (Exception ex) {
                // Ловим непредвиденные ошибки, логируем и возвращаем 500
                log.error("Unexpected error in JWT authentication filter", ex);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Internal server error\"}");
                return;
            }
        }

        // Передаем управление дальше по цепочке фильтров
        filterChain.doFilter(request, response);
    }

    private void tryAuthenticate(HttpServletRequest request) {
        String token = extractFromCookie(request, accessCookieName);

        if (token == null && allowAuthorizationHeaderFallback) {
            token = extractFromAuthorizationHeader(request);
        }
        if (token == null) {
            return;
        }

        // Валидируем и парсим ровно ОДИН раз (не делаем double-parse)
        final Claims claims;
        try {
            claims = accessTokenService.validate(token);
        } catch (ExpiredJwtException e) {
            throw new CredentialsExpiredException("Access token expired", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Access token is invalid", e);
        }

        String subject = claims.getSubject();
        if (!StringUtils.hasText(subject)) {
            throw new BadCredentialsException("Token has no subject");
        }

        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            // subject не UUID — не наш формат
            throw new BadCredentialsException("Token subject is not a valid UUID", ex);
        }

        AuthUserData user;
        try {
            user = authDetailsService.findById(userId);
        } catch (Exception e) {
            throw new BadCredentialsException("User not found for subject", e);
        }

        if (user == null || user.getStatus() != AccountStatus.ACTIVE) {
            throw new BadCredentialsException("User is null or not ACTIVE");
        }
        System.out.println("ПРОШЕЛ ФИЛЬТР");
        System.out.println("ПРОШЕЛ ФИЛЬТР");
        System.out.println("ПРОШЕЛ ФИЛЬТР");
        System.out.println("ПРОШЕЛ ФИЛЬТР");
        System.out.println("ПРОШЕЛ ФИЛЬТР");
        System.out.println("ПРОШЕЛ ФИЛЬТР");
        System.out.println("ПРОШЕЛ ФИЛЬТР");



        // Собираем authorities: ROLE_*, а также permissions
        var authorities = toAuthorities(user);

        AbstractAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Достаёт токен из HttpOnly cookie по имени.
     * Ищем куку по имени, возвращаем значение из нее или null
     */
    private static String extractFromCookie(HttpServletRequest request, String cookieName) {
        return Optional.ofNullable(request.getCookies())
                .stream()
                .flatMap(Arrays::stream)
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    /**
     * Опциональный fallback — достать токен из заголовка Authorization: Bearer ...
     */
    private static String extractFromAuthorizationHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header)) return null;
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) return null; // case-insensitive "Bearer "
        String token = header.substring(7);
        return StringUtils.hasText(token) ? token.trim() : null;
    }

    /**
     * объединяем роли и permissions в GrantedAuthority (в один список).
     */
    private static Collection<? extends GrantedAuthority> toAuthorities(AuthUserData user) {
        // роли уже возвращают "ROLE_xxx" в getAuthority()
        Stream<RoleDto> roles = user.getRoles().stream();

        // права можно собирать напрямую, distinct по equals/hashCode
        Stream<PermissionDto> perms = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream());

        return Stream.concat(roles, perms)
                .filter(Objects::nonNull)
                .filter(a -> StringUtils.hasText(a.getAuthority()))
                .distinct()
                .toList();
    }
}
