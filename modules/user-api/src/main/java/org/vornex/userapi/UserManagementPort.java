package org.vornex.userapi;

import java.util.Optional;
import java.util.UUID;

public interface UserManagementPort { // для бизнес логики
    UserAccountDto create(UserAccountDto createDto);
    UserAccountDto findById(UUID id);
    UserAccountDto findByUsername(String userName);
    UserAccountDto findByEmail(String email);
    UserAccountDto findByPhoneNumber(String phoneNumber);
    Optional<UserAccountDto> findByEmailOrNumber(UserAccountDto accountDto);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

}
