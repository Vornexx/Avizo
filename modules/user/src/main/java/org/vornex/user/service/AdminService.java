package org.vornex.user.service;

import org.vornex.user.dto.internal.UserDto;
import org.vornex.user.dto.request.UserFilterDto;
import org.vornex.user.dto.response.PagedResponse;

public interface AdminService {
    PagedResponse<UserDto> getAllUsers(int page, int size, UserFilterDto filterDto);
}
