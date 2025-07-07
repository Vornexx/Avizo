package org.vornex.userapi;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionDto implements GrantedAuthority {
    private String name;

    @Override
    public String getAuthority() {
        return name;
    }
}