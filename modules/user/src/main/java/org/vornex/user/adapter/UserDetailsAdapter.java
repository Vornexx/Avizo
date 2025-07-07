package org.vornex.user.adapter;

import lombok.AllArgsConstructor;
import org.vornex.authapi.AuthUserData;
import org.vornex.user.entity.User;
import org.vornex.userapi.AccountStatus;
import org.vornex.userapi.PermissionDto;
import org.vornex.userapi.RoleDto;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
public class UserDetailsAdapter implements AuthUserData {
    private final User user;

    @Override
    public UUID getId() {
        return user.getId();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public AccountStatus getStatus() {
        return user.getStatus();
    }

    @Override
    public Set<RoleDto> getRoles() {
        return user.getRoles().stream()
                .map(role -> RoleDto.builder()
                        .name(role.getName())
                        .permissions(role.getPermissions().stream()
                                .map(p -> PermissionDto.builder().name(p.getName()).build())
                                .collect(Collectors.toSet()))
                        .build())
                .collect(Collectors.toSet());
    }
}
