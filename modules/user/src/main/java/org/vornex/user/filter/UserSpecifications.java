package org.vornex.user.filter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.vornex.user.entity.Role;
import org.vornex.user.entity.User;
import org.vornex.userapi.AccountStatus;

public class UserSpecifications {
    public static Specification<User> hasStatus(AccountStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<User> hasCity(String city) {
        return (root, query, cb) -> cb.equal(root.get("city"), city);
    }

    public static Specification<User> hasRole(String roleName) {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                query.distinct(true);
            }
            Join<User, Role> roles = root.join("roles", JoinType.INNER);
            return cb.equal(roles.get("name"), roleName);
        };
    }
}