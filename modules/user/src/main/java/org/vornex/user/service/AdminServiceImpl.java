package org.vornex.user.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.vornex.user.dto.internal.UserDto;
import org.vornex.user.dto.request.UserFilterDto;
import org.vornex.user.dto.response.PagedResponse;
import org.vornex.user.entity.Role;
import org.vornex.user.entity.User;
import org.vornex.user.filter.UserSpecifications;
import org.vornex.user.mapper.UserMapper;
import org.vornex.user.repository.RoleRepository;
import org.vornex.user.repository.UserRepository;
import org.vornex.userapi.AccountStatus;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final RoleRepository roleRepository;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public PagedResponse<UserDto> getAllUsers(int page, int size, UserFilterDto filterDto) {
        int validatedSize = Math.min(size, MAX_PAGE_SIZE);
        int validatedPage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by("createdAt").descending());

        Specification<User> spec = buildSpecification(filterDto);

        // Подгружаем роли сразу, чтобы не было n+1
        Page<User> pageResult = userRepository.findAll(spec, pageable);

        List<UserDto> content = pageResult.stream()
                .map(mapper::toUserDto)
                .toList();

        return PagedResponse.<UserDto>builder()
                .content(content)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();

    }

    @Override
    @Transactional(readOnly = true) //оптимизация hibernate чтобы не следил за dirty-checking
    public UserDto getUserById(UUID id) {
        User user = userRepository.findByIdWithRolesAndPermissions(id) // чтобы в dto смапился
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapper.toUserDto(user);
    }

    @Override
    public void changeActive(UUID id, AccountStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (status != null) {
            user.setStatus(status);
        }
    }

    @Override
    public void updateUserRoles(UUID id, Set<String> roleNames) {
        User user = userRepository.findByIdWithRolesAndPermissions(id) //
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<Role> roles = roleRepository.findByNameIn(roleNames);

        if (roles.size() != roleNames.size()) { // если кол-во запрошенных ролей != кол-во полученных ролей
            Set<String> found = roles.stream().map(Role::getName).collect(Collectors.toSet());
            Set<String> missing = new HashSet<>(roleNames); // делаем копию. defensive copy чтобы не мутировать входные данные
            missing.removeAll(found); // теперь безопасно модифицируем
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Роли не найдены:" + String.join(", ", missing)); // join чтобы не отображалось с квадратными скобками.
        }
        user.setRoles(new HashSet<>(roles));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changeAccountStatus(UUID id, String newStatusStr) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        AccountStatus currentStatus = user.getStatus();

        AccountStatus newStatus;
        try {
            newStatus = AccountStatus.valueOf(newStatusStr.toUpperCase()); // конвертируем в enum
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Статус не существует: " + newStatusStr);
        }

        if (currentStatus == newStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Статус уже установлен: " + newStatus);
        }

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Переход из " + currentStatus + " в " + newStatus + " не разрешён");
        }

        user.setStatus(newStatus);
        userRepository.save(user);
    }

    public Specification<User> buildSpecification(UserFilterDto filter) {
        List<Specification<User>> specs = new ArrayList<>();

        if (filter.getStatus() != null) {
            specs.add(UserSpecifications.hasStatus(filter.getStatus()));
        }
        if (filter.getCity() != null && !filter.getCity().isEmpty()) {
            specs.add(UserSpecifications.hasCity(filter.getCity()));
        }
        if (filter.getRoleName() != null && !filter.getRoleName().isEmpty()) {
            specs.add(UserSpecifications.hasRole(filter.getRoleName()));
        }

        return specs.stream().reduce(Specification::and).orElse(null);
    }
}
