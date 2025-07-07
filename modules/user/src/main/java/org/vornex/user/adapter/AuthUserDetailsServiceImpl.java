package org.vornex.user.adapter;

import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.vornex.authapi.AuthDetailsService;
import org.vornex.authapi.AuthUserData;
import org.vornex.user.entity.User;
import org.vornex.user.repository.UserRepository;

import java.util.Optional;
@AllArgsConstructor
@Service
public class AuthUserDetailsServiceImpl implements AuthDetailsService {
    private final UserRepository userRepository;

    @Override
    public AuthUserData findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserDetailsAdapter(user);
    }
}
