package org.vornex.authapi;


public interface AuthDetailsService {
    AuthUserData findByUsername(String username);
}
