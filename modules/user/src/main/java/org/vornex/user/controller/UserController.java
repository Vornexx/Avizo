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
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

// -----------> ПОЛУЧЕНИЕ ИНФОРМАЦИИ О ПОЛЬЗОВАТЕЛЕ <-----------

    @GetMapping("/me")
    // CustomUserDetails содержит в себе AuthUserData поэтому мы тут из контекста поднимает самого юзера (через аргумент резолвер).
    public ResponseEntity<UserProfileDto> getMyProfile(@AuthenticationPrincipal AuthUserData userDetails) {
        return ResponseEntity.ok(userService.getMyProfile(userDetails));
    }

    //id пользователя будет в карточке объявления (listing)
    @GetMapping("/users/{id}")
    public ResponseEntity<PublicUserDto> getPublicUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getPublicUserById(id));
    }

    // -----------> ОБНОВЛЕНИЕ ПРОФИЛЯ ПОЛЬЗОВАТЕЛЯ <-----------
    @PutMapping("/me")
    public ResponseEntity<Void> updateMyProfile(@AuthenticationPrincipal AuthUserData userDetails,
                                                @Valid @RequestBody UpdateProfileDto updateProfileDto) {
        userService.updateMyProfile(userDetails.getId(), updateProfileDto);
        return ResponseEntity.ok().build();
    }
    //инвалидировать старые токены, решить вопрос с jwt перед тем как этой хуйней заниматься.
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthUserData userDetails, @Valid @RequestBody ChangePasswordDto passwordDto) {
        userService.changePassword(userDetails.getId(), passwordDto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/email")
    public ResponseEntity<Void> changeEmail(@AuthenticationPrincipal AuthUserData userDetails, @Valid @RequestBody ChangeEmailDto emailDto) {
        userService.changeEmail(userDetails.getId(), emailDto);
        return ResponseEntity.ok().build();
    }

    //Можно добавить валидацию MX-записи email через стороннюю либу.
    //
    //Можно отправить подтверждение на новый email (через токен-ссылку).
    //
    //Можно логировать смену email'а в audit.log.

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal AuthUserData userDetails) {
        userService.deleteAccount(userDetails.getId());
        return ResponseEntity.ok().build();
    }
//    // в будущем добавить смену аватара

    // -----------> ИЗБРАННОЕ ОБЪЯВЛЕНИЙ <-----------




}