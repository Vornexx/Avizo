package org.vornex.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.vornex.authapi.AuthDetailsService;
import org.vornex.authapi.AuthUserData;
import org.vornex.userapi.AccountStatus;


@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final AuthDetailsService delegate;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUserData user = delegate.findByUsername(username); // проверка Not found в user реализации.

        if (user.getStatus() == AccountStatus.BANNED) {
            throw new LockedException("Your account is banned");
        }
        if (user.getStatus() == AccountStatus.DELETED) {
            throw new DisabledException("AccountDeleted");
        }
        return new CustomUserDetails(user);
    }
}
