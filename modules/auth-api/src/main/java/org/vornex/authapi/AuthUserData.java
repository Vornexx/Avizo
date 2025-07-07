package org.vornex.authapi;

import org.vornex.userapi.AccountStatus;
import org.vornex.userapi.RoleDto;

import java.util.Set;
import java.util.UUID;

public interface AuthUserData {
    UUID getId();
    String getUsername();
    String getPassword();
    AccountStatus getStatus();
    Set<RoleDto> getRoles();
}
