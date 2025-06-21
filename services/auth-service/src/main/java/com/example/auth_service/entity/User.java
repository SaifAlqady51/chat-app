package com.example.auth_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_email", columnList = "email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "status", length = 10)
    @Builder.Default
    private String status = "offline";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_active")
    private LocalDateTime lastActive;
}
