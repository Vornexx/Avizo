package org.vornex.user.service;

import org.vornex.user.dto.internal.UserDto;
import org.vornex.user.dto.request.UserFilterDto;
import org.vornex.user.dto.response.PagedResponse;
import org.vornex.userapi.AccountStatus;

import java.util.Set;
import java.util.UUID;

public interface AdminService {
    PagedResponse<UserDto> getAllUsers(int page, int size, UserFilterDto filterDto);

    UserDto getUserById(UUID id);

    void changeActive(UUID id, AccountStatus status);

    void updateUserRoles(UUID id, Set<String> roles);

    void changeAccountStatus(UUID id, String newStatusStr);


}
