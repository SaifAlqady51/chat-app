package com.example.auth_service.service.validator;

import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.dto.RefreshTokenRequest;
import com.example.auth_service.entity.User;
import org.springframework.web.server.ResponseStatusException;

public interface AuthValidator {
    void validateRegisterRequest(RegisterRequest request) throws ResponseStatusException;
    void validateLoginRequest(LoginRequest request) throws ResponseStatusException;
    void validateRefreshTokenRequest(RefreshTokenRequest request) throws ResponseStatusException;
}