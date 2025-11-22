package org.vornex.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.vornex.authapi.AuthUserData;
import org.vornex.user.dto.internal.UpdateProfileDto;
import org.vornex.user.dto.request.ChangeEmailDto;
import org.vornex.user.dto.request.ChangePasswordDto;
import org.vornex.user.dto.response.PublicUserDto;
import org.vornex.user.dto.response.UserProfileDto;
import org.vornex.user.service.UserService;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

// -----------> ПОЛУЧЕНИЕ ИНФОРМАЦИИ О ПОЛЬЗОВАТЕЛЕ <-----------

    @GetMapping("/me")
    // CustomUserDetails содержит в себе AuthUserData поэтому мы тут из контекста поднимает самого юзера (через аргумент резолвер).
    public ResponseEntity<UserProfileDto> getMyProfile(@AuthenticationPrincipal AuthUserData userDetails) {
        return ResponseEntity.ok(userService.getMyProfile(userDetails));
    }

    //id пользователя будет в карточке объявления (listing)
    @GetMapping("/{id}")
    public ResponseEntity<PublicUserDto> getPublicUserById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(userService.getPublicUserById(id));
    }

    // -----------> ОБНОВЛЕНИЕ ПРОФИЛЯ ПОЛЬЗОВАТЕЛЯ <-----------
    @PutMapping("/me")
    public ResponseEntity<Void> updateMyProfile(@AuthenticationPrincipal AuthUserData userDetails,
                                                @Valid @RequestBody UpdateProfileDto updateProfileDto) {
        userService.updateMyProfile(userDetails.getId(), updateProfileDto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthUserData userDetails, @Valid @RequestBody ChangePasswordDto passwordDto) {
        userService.changePassword(userDetails.getId(), passwordDto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/email")
    public ResponseEntity<Void> changeEmail(@AuthenticationPrincipal AuthUserData userDetails, @Valid @RequestBody ChangeEmailDto emailDto) {
        userService.changeEmail(userDetails.getId(), emailDto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal AuthUserData userDetails) {
        userService.deleteAccount(userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}