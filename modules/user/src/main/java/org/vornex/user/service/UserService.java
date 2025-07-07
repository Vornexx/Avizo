package org.vornex.user.service;

import org.vornex.authapi.AuthUserData;
import org.vornex.user.dto.internal.UpdateProfileDto;
import org.vornex.user.dto.request.ChangeEmailDto;
import org.vornex.user.dto.request.ChangePasswordDto;
import org.vornex.user.dto.response.PublicUserDto;
import org.vornex.user.dto.response.UserProfileDto;

import java.util.UUID;

public interface UserService {
    UserProfileDto getMyProfile(AuthUserData userDetails);

    PublicUserDto getPublicUserById(UUID uuid);

    void updateMyProfile(UUID id, UpdateProfileDto updateProfileDto);

    void changePassword(UUID id, ChangePasswordDto passwordDto);

    void changeEmail(UUID id, ChangeEmailDto emailDto);

    void deleteAccount(UUID id);

}
