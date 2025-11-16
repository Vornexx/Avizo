package org.vornex.user.adapter;

import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.vornex.authapi.AuthDetailsService;
import org.vornex.authapi.AuthUserData;
import org.vornex.user.entity.User;
import org.vornex.user.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Service
public class AuthUserDetailsServiceImpl implements AuthDetailsService {
    private final UserRepository userRepository;


    @Override
    public AuthUserData findByUsername(String username) {
        User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return UserDetailsAdapter.fromUser(user);
    }

    @Override
    public AuthUserData findById(UUID userId) {
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return UserDetailsAdapter.fromUser(user);
    }
}
