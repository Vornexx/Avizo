package org.vornex.user.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.vornex.user.entity.Role;
import org.vornex.user.entity.User;
import org.vornex.user.mapper.UserMapper;
import org.vornex.user.repository.RoleRepository;
import org.vornex.user.repository.UserRepository;
import org.vornex.userapi.UserAccountDto;
import org.vornex.userapi.UserManagementPort;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserManagementPortAdapter implements UserManagementPort {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper mapper;

    @Override
    @Transactional
    public UserAccountDto create(UserAccountDto createDto) {
        User user = mapper.userFromDto(createDto);

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default role USER not found"));

        // Устанавливаем изменяемое множество ролей ДО сохранения
        user.setRoles(new HashSet<>(Collections.singletonList(userRole)));

        User saved = userRepository.save(user); // persist + flush в рамках транзакции
        return mapper.toUserAccountDto(saved);
    }

    @Override
    @Transactional(readOnly = true) // отключает dirty checking, оптмизирует работу с журналами транзакций.
    public UserAccountDto findById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapper.toUserAccountDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccountDto findByUsername(String userName) {
        User user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapper.toUserAccountDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccountDto findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapper.toUserAccountDto(user);
    }

    @Override
    public UserAccountDto findByPhoneNumber(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapper.toUserAccountDto(user);
    }

    @Override
    public Optional<UserAccountDto> findByEmailOrNumber(UserAccountDto accountDto) {
        return Optional.ofNullable(
                StringUtils.hasText(accountDto.phoneNumber())
                        ? userRepository.findByPhoneNumber(accountDto.phoneNumber()).orElse(null)
                        : StringUtils.hasText(accountDto.email())
                        ? userRepository.findByEmail(accountDto.email()).orElse(null)
                        : null
        ).map(mapper::toUserAccountDto);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
