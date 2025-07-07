package org.vornex.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.vornex.user.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

//    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
//    Optional<User> findByUsernameWithRolesAndPermissions(String username);
//
//    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
//    Optional<User> findByIdWithRolesAndPermissions(UUID id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.username = :username")
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("username") String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.id = :id")
    Optional<User> findByIdWithRolesAndPermissions(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"roles"})
    @NonNull
    Page<User> findAll( Specification<User> spec, @NonNull Pageable pageable);

    boolean existsByEmail(String email);


}
