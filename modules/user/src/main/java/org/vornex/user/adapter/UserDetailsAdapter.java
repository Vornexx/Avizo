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
    private final UUID id;
    private final String username;
    private final String password;
    private final AccountStatus status;
    private final Set<RoleDto> roles;

    // фабричный метод из User, где уже eager загружены роли и permissions
    public static UserDetailsAdapter fromUser(User user) {
        Set<RoleDto> roleDtos = user.getRoles().stream()
                .map(role -> RoleDto.builder()
                        .name(role.getName())
                        .permissions(role.getPermissions().stream()
                                .map(p -> PermissionDto.builder()
                                        .name(p.getName())
                                        .build())
                                .collect(Collectors.toSet()))
                        .build())
                .collect(Collectors.toSet());

        return new UserDetailsAdapter(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getStatus(),
                roleDtos
        );
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public String getUsername() { return username; }

    @Override
    public String getPassword() { return password; }

    @Override
    public AccountStatus getStatus() { return status; }

    @Override
    public Set<RoleDto> getRoles() { return roles; }

}
