package org.vornex.userapi;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDto implements GrantedAuthority {
    private String name;
    private Set<PermissionDto> permissions;

    @Override
    public String getAuthority() {
        return "ROLE_" + name;
    }
}