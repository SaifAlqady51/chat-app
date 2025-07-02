package com.example.auth_service.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private UUID userId;
    private String username;
    private String email;
    private String avatarUrl;
    private String status;
    private Instant expiresAt;
}
