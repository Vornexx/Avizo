package org.vornex.authapi;


import java.util.UUID;

public interface AuthDetailsService { //для security логики
    AuthUserData findByUsername(String username);
    AuthUserData findById(UUID userId);
}
