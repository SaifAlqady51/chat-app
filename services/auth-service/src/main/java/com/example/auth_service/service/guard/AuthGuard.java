package com.example.auth_service.service.guard;

import com.example.auth_service.dto.RegisterRequest;
import org.springframework.web.server.ResponseStatusException;

public interface AuthGuard {
    void checkRegistrationEligibility(RegisterRequest request) throws ResponseStatusException;
   // void validateLoginRequest(LoginRequest request) throws ResponseStatusException;
    //void validateRefreshTokenRequest(RefreshTokenRequest request) throws ResponseStatusException;
}