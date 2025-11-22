package org.vornex.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.vornex.auth.controller.AuthController;
import org.vornex.auth.dto.TokenPair;
import org.vornex.auth.dto.UserLoginDto;
import org.vornex.auth.service.AuthService;
import org.vornex.auth.util.RefreshTokenExtractor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthController focusing on cookie creation.
 * Мы не поднимаем web-контекст — тестируем в чистом виде.
 */
class AuthControllerUnitTest {

    @Mock
    private AuthService authService; // интерфейс Mockito обычно легко создаёт прокси для интерфейсов

    @Mock
    private RefreshTokenExtractor refreshTokenExtractor;

    // Тесты становятся более стабильными: Mockito не пытается менять байт-код конкретной реализации.


    private AutoCloseable mocks;

    private final String accessCookieName = "ACCESS_COOKIE";
    private final String refreshCookieName = "REFRESH_COOKIE";

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void close() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Test
    void login_shouldSetTwoCookiesAndReturn204() {
        // Arrange
        Clock fixed = Clock.fixed(Instant.parse("2025-09-01T10:00:00Z"), ZoneOffset.UTC);
        AuthController controller = new AuthController(authService, fixed, accessCookieName, refreshCookieName, refreshTokenExtractor);

        UserLoginDto dto = UserLoginDto.builder()
                .email("user@example.com")
                .password("pwd")
                .build();

        // Токены с валидными expiry
        Instant accessExp = fixed.instant().plus(Duration.ofMinutes(15));
        Instant refreshExp = fixed.instant().plus(Duration.ofDays(7));
        TokenPair tokens = new TokenPair("access.token", "refresh.token", accessExp, refreshExp);

        when(authService.login(dto)).thenReturn(tokens);

        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        ResponseEntity<Void> result = controller.login(dto, response);

        // Assert
        assertThat(result.getStatusCodeValue()).isEqualTo(204);
        // В ответе должны быть два Set-Cookie заголовка (access и refresh)
        String[] setCookie = response.getHeaders(HttpHeaders.SET_COOKIE).toArray(new String[0]);
        assertThat(setCookie).hasSizeGreaterThanOrEqualTo(2);
        // Убедимся, что в куках есть имена, которые мы передали
        assertThat(String.join(";", setCookie)).contains(accessCookieName).contains(refreshCookieName);

        verify(authService).login(dto);
    }

    @Test
    void login_withExpiredToken_shouldThrowIllegalStateException() {
        // Если один из expiry уже в прошлом — createHttpOnlyCookie бросит IllegalStateException.
        Clock fixed = Clock.fixed(Instant.parse("2025-09-01T10:00:00Z"), ZoneOffset.UTC);
        AuthController controller = new AuthController(authService, fixed, accessCookieName, refreshCookieName, refreshTokenExtractor);

        UserLoginDto dto = UserLoginDto.builder()
                .email("user@example.com")
                .password("pwd")
                .build();

        // access уже просрочен
        TokenPair tokens = new TokenPair("access.token", "refresh.token",
                fixed.instant().minus(Duration.ofSeconds(1)),
                fixed.instant().plus(Duration.ofDays(7)));

        when(authService.login(dto)).thenReturn(tokens);

        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act & Assert
        assertThatThrownBy(() -> controller.login(dto, response))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Token already expired");
    }
}
