package org.vornex.auth.service;

import org.vornex.auth.dto.TokenPair;
import org.vornex.auth.dto.UserLoginDto;
import org.vornex.auth.dto.UserRegistrationDto;
import org.vornex.userapi.UserAccountDto;

public interface AuthService {
    TokenPair login(UserLoginDto loginDto);

    TokenPair register(UserRegistrationDto regDto);

    TokenPair refreshTokens(String refreshToken);

    void logout(String userId);
}
