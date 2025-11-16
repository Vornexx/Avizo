package org.vornex.user.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.vornex.authapi.AuthUserData;
import org.vornex.user.dto.internal.UpdateProfileDto;
import org.vornex.user.dto.request.ChangeEmailDto;
import org.vornex.user.dto.request.ChangePasswordDto;
import org.vornex.user.dto.response.PublicUserDto;
import org.vornex.user.dto.response.UserProfileDto;
import org.vornex.user.entity.User;
import org.vornex.user.mapper.UserMapper;
import org.vornex.user.repository.UserRepository;
import org.vornex.userapi.AccountStatus;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileDto getMyProfile(AuthUserData userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapper.toUserProfilesDto(user);
    }

    @Override
    public PublicUserDto getPublicUserById(UUID uuid) {
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account is not active");
        }
        return mapper.toPublicUserDto(user);
    }

    @Override
    @Transactional // нужна для ВСЕХ которые меняют данные, для тех, что только читают не надо.
    public void updateMyProfile(UUID id, UpdateProfileDto updateProfileDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        //из dto сеттит в user.
        mapper.updateFromDto(updateProfileDto, user);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(UUID id, ChangePasswordDto passwordDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(passwordDto.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "текущий пароль не верный");
        }
        String encoderNewPassword = passwordEncoder.encode(passwordDto.getNewPassword());
        user.setPassword(encoderNewPassword);
    }

    @Override
    @Transactional
    public void changeEmail(UUID id, ChangeEmailDto emailDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getEmail().equals(emailDto.getNewEmail())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email совпадает с текущим");
        }
        if (userRepository.existsByEmail(emailDto.getNewEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email уже используется");
        }
        user.setEmail(emailDto.getNewEmail());
    }

    @Override
    @Transactional
    public void deleteAccount(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Удаление возможно только для активных аккаунтов");
        }
        user.setStatus(AccountStatus.DELETED);
    }

}
