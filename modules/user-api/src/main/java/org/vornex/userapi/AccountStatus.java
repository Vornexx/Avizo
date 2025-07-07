package org.vornex.userapi;

import java.util.Set;

public enum AccountStatus {
    ACTIVE,
    BANNED,
    DELETED;

    private Set<AccountStatus> allowedTransitions;

    static {
        ACTIVE.allowedTransitions = Set.of(BANNED, DELETED);
        BANNED.allowedTransitions = Set.of(ACTIVE, DELETED);
        DELETED.allowedTransitions = Set.of(ACTIVE);
    }

    public boolean canTransitionTo(AccountStatus newStatus) {
        return allowedTransitions.contains(newStatus);
    }
}
