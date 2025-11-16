package org.vornex.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vornex.auth.filter.JwtAuthenticationFilter;
import org.vornex.auth.service.AccessTokenService;
import org.vornex.authapi.AuthDetailsService;
import org.vornex.authapi.AuthUserData;
import org.vornex.userapi.AccountStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit test for JwtAuthenticationFilter.
 *
 * Подход: мокируем AccessTokenService и AuthDetailsService. Создаём MockHttpServletRequest
 * с одной кукой. Вызываем doFilterInternal и проверяем SecurityContext.
 */
class JwtAuthenticationFilterTest {

//    @Mock private AccessTokenService accessTokenService;
//    @Mock private AuthDetailsService authDetailsService;
//    @Mock private FilterChain filterChain;
//
//    @BeforeEach
//    void init() {
//        MockitoAnnotations.openMocks(this);
//        SecurityContextHolder.clearContext();
//    }
//
//    @AfterEach
//    void tearDown() {
//        SecurityContextHolder.clearContext();
//    }
//
//    @Test
//    void doFilterInternal_withValidCookie_setsAuthentication() throws Exception {
//        String cookieName = "ACCESS";
//        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(accessTokenService, authDetailsService, cookieName, false, null);
//
//        MockHttpServletRequest req = new MockHttpServletRequest();
//        MockHttpServletResponse res = new MockHttpServletResponse();
//        req.setCookies(new jakarta.servlet.http.Cookie(cookieName, "valid.token"));
//
//        // Мокаем Claims (subject == uuid)
//        Claims claims = mock(Claims.class);
//        when(claims.getSubject()).thenReturn(UUID.randomUUID().toString());
//
//        when(accessTokenService.validate("valid.token")).thenReturn(claims);
//
//        AuthUserData user = mock(AuthUserData.class);
//        when(user.getStatus()).thenReturn(AccountStatus.ACTIVE);
//        when(authDetailsService.findById(any())).thenReturn(user);
//
//        // Act
//        filter.doFilter(req, res, filterChain); // меняем на doFilter он под капотом вызовет внутренний doFilterInternal
//
//        // Assert
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        assertThat(auth).isNotNull();
//        assertThat(auth.isAuthenticated()).isTrue();
//        assertThat(auth.getPrincipal()).isEqualTo(user);
//    }
//
//    @Test
//    void doFilterInternal_withInvalidToken_doesNotSetAuthentication() throws Exception {
//        String cookieName = "ACCESS";
//        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(accessTokenService, authDetailsService, cookieName, false, null);
//
//        MockHttpServletRequest req = new MockHttpServletRequest();
//        MockHttpServletResponse res = new MockHttpServletResponse();
//        req.setCookies(new jakarta.servlet.http.Cookie(cookieName, "invalid.token"));
//
//        when(accessTokenService.validate("invalid.token")).thenThrow(new RuntimeException("invalid"));
//
//        filter.doFilter(req, res, filterChain);
//
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        assertThat(auth).isNull();
//    }
}
