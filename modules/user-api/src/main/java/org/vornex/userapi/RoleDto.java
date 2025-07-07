package org.vornex.userapi;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDto {
    private String name;
    private Set<PermissionDto> permissions;
}