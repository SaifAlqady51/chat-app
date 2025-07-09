package com.example.auth_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.auth_service.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    // Standard method to check if email exists
    boolean existsByEmail(String email);

    // Method to find user by email
    Optional<User> findByEmail(String email);

    // Native query version if needed
    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)", nativeQuery = true)
    boolean nativeExistsByEmail(@Param("email") String email);

    // Flush and check existence if needed
    default boolean existsByEmailWithFlush(String email) {
        flush();
        return existsByEmail(email);
    }
}