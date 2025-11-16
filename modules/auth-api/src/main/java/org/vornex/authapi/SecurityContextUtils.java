package org.vornex.authapi;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.vornex.exception.UnauthorizedException;

import java.util.Optional;
import java.util.UUID;

/**
 * Простая реализация SecurityFacade.
 * <p>
 * Политика извлечения userId:
 * - если principal instanceof Jwt -> берём claim "sub" и пробуем распарсить как UUID;
 * - иначе если principal instanceof String -> пробуем распарсить как UUID.
 * <p>
 * Только эти два варианта — ни UserDetails, ни дополнительные fallback'ы.
 */
@Component
public class SecurityContextUtils {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";


//    public Optional<UUID> getCurrentUserId() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
//
//        Object principal = auth.getPrincipal();
//        try {
////            if (principal instanceof Jwt jwt) {
////                Object sub = jwt.getClaim("sub");
////                if (sub != null) {
////                    return Optional.of(UUID.fromString(String.valueOf(sub)));
////                }
////                return Optional.empty();
////            }
//
//            if (principal instanceof String s) {
//                return Optional.of(UUID.fromString(s));
//            }
//            // никаких других вариантов — минимализм
//            return Optional.empty();
//        } catch (IllegalArgumentException ex) {
//            // не удалось распарсить UUID
//            return Optional.empty();
//        }
//    }

public Optional<UUID> getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return Optional.empty();

    Object principal = auth.getPrincipal();
    try {
        if (principal instanceof AuthUserData user) {
            return Optional.of(UUID.fromString(user.getId().toString()));
        }

        if (principal instanceof String s) { // fallback, если где-то String
            return Optional.of(UUID.fromString(s));
        }

        return Optional.empty();
    } catch (IllegalArgumentException ex) {
        return Optional.empty();
    }
}

    /**
     * Возвращает UUID текущего пользователя или выбрасывает UnauthorizedException,
     * если пользователь не аутентифицирован или id некорректен.
     *
     * Используй в методах, где аутентификация обязательна.
     * Вообще у нас и так проверка на jwt access key в фильтре, но gpt говорит на всякий случай
     */
    public UUID getCurrentUserIdRequired() {
        return getCurrentUserId().orElseThrow(() -> new UnauthorizedException("Authenticated user id required"));
    }


    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (ROLE_ADMIN.equals(ga.getAuthority())) return true;
        }
        return false;
    }
}

