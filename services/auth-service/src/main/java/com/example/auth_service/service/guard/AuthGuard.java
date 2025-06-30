package com.example.auth_service.service.guard;

import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.entity.User;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

public interface AuthGuard {
    void checkRegistrationEligibility(RegisterRequest request) throws ResponseStatusException;
    Optional<User> checkLoginAndGetUser(LoginRequest request) throws ResponseStatusException;
}