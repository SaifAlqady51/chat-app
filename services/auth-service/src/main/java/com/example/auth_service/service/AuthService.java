package com.example.auth_service.service;

import com.example.auth_service.dto.*;

import java.util.UUID;

public interface AuthService {
    ApiResponse<AuthResponse> register(RegisterRequest request);

    ApiResponse<AuthResponse> login(LoginRequest request);

    ApiResponse<AuthResponse> refreshToken(RefreshTokenRequest request);

    ApiResponse<Void> logout(LogoutRequest request);

    ApiResponse<Boolean> checkUserExistsById(UUID userId);
}
