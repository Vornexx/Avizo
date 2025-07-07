package org.vornex.user.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.vornex.user.dto.internal.UserDto;
import org.vornex.user.dto.request.UserFilterDto;
import org.vornex.user.dto.response.PagedResponse;
import org.vornex.user.entity.User;
import org.vornex.user.filter.UserSpecifications;
import org.vornex.user.mapper.UserMapper;
import org.vornex.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository;
    private final UserMapper mapper;
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
