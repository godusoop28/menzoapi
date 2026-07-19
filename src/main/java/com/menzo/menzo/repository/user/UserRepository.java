package com.menzo.menzo.repository.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.menzo.menzo.domain.user.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("""
            SELECT u FROM User u
            WHERE lower(u.displayName) LIKE lower(concat('%', :query, '%'))
               OR lower(u.username) LIKE lower(concat('%', :query, '%'))
            """)
    Page<User> search(@Param("query") String query, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.online = false WHERE u.online = true AND u.lastActiveAt < :threshold")
    int markStaleUsersOffline(@Param("threshold") Instant threshold);

    List<User> findAllByOrderByReputationDesc(Pageable pageable);
}
