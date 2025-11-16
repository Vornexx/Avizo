package org.vornex.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.vornex.auth.dto.TokenPair;
import org.vornex.auth.dto.UserLoginDto;
import org.vornex.auth.dto.UserRegistrationDto;
import org.vornex.auth.mapper.AdapterUserMapper;
import org.vornex.auth.service.AccessTokenService;
import org.vornex.auth.service.RefreshTokenService;
import org.vornex.auth.service.impl.AuthServiceImpl;
import org.vornex.userapi.UserAccountDto;
import org.vornex.userapi.UserManagementPort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 * Мокаем все внешние зависимости (ports, token services, mapper).
 */
class AuthServiceImplUnitTest {

    @Mock
    private AdapterUserMapper mapper;
    @Mock
    private UserManagementPort userService;
    @Mock
    private AccessTokenService accessTokenService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordEncoder encoder;

    private Clock clock = Clock.fixed(Instant.parse("2025-09-01T10:00:00Z"), ZoneOffset.UTC);
    private AuthServiceImpl service;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        service = new AuthServiceImpl(mapper, userService, accessTokenService,
                refreshTokenService, encoder, clock);
    }

    @Test
    void login_whenCredentialsValid_returnsTokenPair() {
        // Arrange
        UserLoginDto loginDto = UserLoginDto.builder()
                .email("user@example.com")
                .password("rawPwd")
                .build();

        // mapper converts login dto to UserAccountDto (containing username/password fields to search)
        // входные данные (для маппера)
        UserAccountDto searchDto = createUserAccount("username", "user@example.com", "rawPassword");


        when(mapper.toUserAccountDto(loginDto)).thenReturn(searchDto);

        // fullUserData simulates record from DB with hashed password
        UserAccountDto fullUserData = createUserAccount("username", "user@example.com", "hashedPassword");

        when(userService.findByEmailOrNumber(searchDto)).thenReturn(Optional.of(fullUserData));

        when(encoder.matches(any(), any())).thenReturn(true);

        // token creation
        when(accessTokenService.generateAccessToken(anyString(), anyMap())).thenReturn("access.token");
        when(refreshTokenService.create(anyString(), anyMap())).thenReturn("refresh.token");

        when(accessTokenService.extractExpiration("access.token")).thenReturn(Optional.of(clock.instant().plusSeconds(900)));
        when(refreshTokenService.extractExpiration("refresh.token")).thenReturn(Optional.of(clock.instant().plusSeconds(86400)));

        // Act
        TokenPair result = service.login(loginDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access.token");
        assertThat(result.refreshToken()).isEqualTo("refresh.token");
    }

    @Test
    void login_whenUserNotFound_throwsBadCredentials() {
        UserLoginDto loginDto = UserLoginDto.builder()
                .email("unknown@example.com")
                .password("pwd")
                .build();

        UserAccountDto searchDto = createUserAccount("unknown", "unknown@example.com", "anyPassword");
        when(mapper.toUserAccountDto(loginDto)).thenReturn(searchDto);
        when(userService.findByEmailOrNumber(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(loginDto))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }

    @Test
    void register_whenUsernameExists_throws409() {
        UserRegistrationDto reg = new UserRegistrationDto();
        reg.setUsername("exists");
        reg.setPassword("password");
        reg.setEmail("x@example.com");
        reg.setFirstName("F");
        reg.setLastName("L");
        reg.setAgreeTerms(true);

        when(userService.existsByUsername("exists")).thenReturn(true);

        assertThatThrownBy(() -> service.register(reg))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void register_onDataIntegrityViolation_mapsTo409() {
        UserRegistrationDto reg = new UserRegistrationDto();
        reg.setUsername("u");
        reg.setPassword("password");
        reg.setEmail("x@example.com");
        reg.setFirstName("F");
        reg.setLastName("L");
        reg.setAgreeTerms(true);

        when(userService.existsByUsername(anyString())).thenReturn(false);
        when(userService.existsByEmail(anyString())).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("hashed");

        UserAccountDto toCreate = createUserAccount("username", "x@example.com", "hashed");

        when(mapper.toUserAccountDto(any(UserRegistrationDto.class), eq("hashed")))
                .thenReturn(toCreate);

        when(userService.create(toCreate)).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> service.register(reg))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }


    private UserAccountDto createUserAccount(
            String username,
            String email,
            String password
    ) {
        return new UserAccountDto(
                UUID.randomUUID(),
                username,
                "First",             // дефолтное имя
                "Last",              // дефолтная фамилия
                email,
                "1234567890",        // дефолтный телефон
                password,
                true                  // agreeTerms по умолчанию
        );
    }

}
