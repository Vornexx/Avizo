package org.vornex.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Optional;

@Component
public class RefreshTokenExtractor {

    private final String cookieName;

    public RefreshTokenExtractor(@Value("${security.auth.refresh-cookie-name}") String cookieName) {
        this.cookieName = cookieName;
    }

    public String extract(HttpServletRequest request) {
        String tokenFromCookie = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        String authHeader = request.getHeader("Authorization");
        return Optional.ofNullable(tokenFromCookie)
                .filter(t -> !t.isBlank())
                .or(() -> Optional.ofNullable(authHeader)
                        .filter(h -> h.startsWith("Bearer "))
                        .map(h -> h.substring(7)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token missing"));
    }
}
