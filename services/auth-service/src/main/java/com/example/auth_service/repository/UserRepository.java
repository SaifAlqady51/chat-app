package com.example.auth_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.auth_service.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)", nativeQuery = true)
    boolean nativeExistsByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.id = :id")
    boolean existsById(@Param("id") UUID id);

    @Query("SELECT u.id FROM User u WHERE u.id IN :userIds")
    List<UUID> findAllExistingUserIds(@Param("userIds") List<UUID> userIds);

    default boolean existsByEmailWithFlush(String email) {
        flush();
        return existsByEmail(email);
    }
}