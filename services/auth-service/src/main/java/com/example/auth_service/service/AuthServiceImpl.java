package com.example.auth_service.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.auth_service.dto.*;
import com.example.auth_service.repository.TokenRepository;
import com.example.auth_service.service.guard.AuthGuard;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.auth_service.entity.User;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service

public class AuthServiceImpl implements AuthService {
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtTokenProvider jwtTokenProvider;
        private final TokenRepository tokenRepository;
        private final AuthGuard authGuard;

        public AuthServiceImpl(
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider,
                        TokenRepository tokenRepository,
                        AuthGuard authGuard
        ) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtTokenProvider = jwtTokenProvider;
                this.tokenRepository = tokenRepository;
                this.authGuard = authGuard;
        }

        @Override
        public ApiResponse<AuthResponse> register(RegisterRequest request) {                        authGuard.checkRegistrationEligibility(request);
                        User user = User.builder()
                                .email(request.getEmail())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .username(request.getUsername())
                                .status("online")
                                .build();

                        userRepository.save(user);

                        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
                        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

                        AuthResponse authResponse =  AuthResponse.builder()
                                .token(accessToken)
                                .refreshToken(refreshToken)
                                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getJwtExpirationInMs()))
                                .userId(user.getId())
                                .username(user.getUsername())
                                .status(user.getStatus())
                                .email(user.getEmail())
                                .build();

                        return ApiResponse.<AuthResponse>builder()
                                .data(authResponse)
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.CREATED.value())
                                .reason(HttpStatus.CREATED.name())
                                .path("api/auth/register")
                                .message("Registration successful").build();

        }

        @Override
        public ApiResponse<AuthResponse> login(LoginRequest request) {
                        User user = authGuard.checkLoginAndGetUser(request)
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED));

                        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
                        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

                        tokenRepository.saveRefreshToken(
                                user.getEmail(),
                                refreshToken,
                                jwtTokenProvider.getRefreshExpirationInMs()
                        );


                        AuthResponse authResponse =  AuthResponse.builder()
                                .token(accessToken)
                                .refreshToken(refreshToken)
                                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getJwtExpirationInMs()))
                                .email(user.getEmail())
                                .userId(user.getId())
                                .username(user.getUsername())
                                .avatarUrl(user.getAvatarUrl())
                                .build();

                        return ApiResponse.<AuthResponse>builder()
                                .data(authResponse)
                                .timestamp(LocalDateTime.now())
                                .reason(HttpStatus.OK.name())
                                .status(HttpStatus.OK.value())
                                .path("api/auth/login")
                                .message("Login successful").build();

        }

        @Override
        public ApiResponse<AuthResponse> refreshToken(RefreshTokenRequest request) {
                        String email = jwtTokenProvider.validateRefreshToken(request.getRefreshToken().toString());

                        User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "User not found"));

                        String newAccessToken = jwtTokenProvider.generateToken(user.getEmail());
                        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

                        tokenRepository.saveRefreshToken(
                                email,
                                newRefreshToken,
                                jwtTokenProvider.getRefreshExpirationInMs()
                        );

                        tokenRepository.addToBlacklist(
                                request.getRefreshToken(),
                                jwtTokenProvider.getRefreshExpirationInMs()
                        );

                        AuthResponse authResponse =  AuthResponse.builder()
                                .token(newAccessToken)
                                .refreshToken(newRefreshToken)
                                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getJwtExpirationInMs()))
                                .email(user.getEmail())
                                .userId(user.getId())
                                .username(user.getUsername())
                                .status(user.getStatus())
                                .build();

                        return ApiResponse.<AuthResponse>builder()
                                .data(authResponse)
                                .timestamp(LocalDateTime.now())
                                .reason(HttpStatus.OK.getReasonPhrase())
                                .status(HttpStatus.OK.value())
                                .path("api/auth/refresh-token")
                                .message("Token refreshed successfully").build();

        }

        @Override
        public ApiResponse<Void> logout(LogoutRequest request) {
                String email = jwtTokenProvider.validateRefreshToken(request.getRefreshToken().toString());

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"));

                tokenRepository.addToBlacklist(
                        request.getToken(),
                        jwtTokenProvider.getJwtExpirationInMs()
                );

                if (request.getRefreshToken() != null && !request.getRefreshToken().isEmpty()) {
                        tokenRepository.addToBlacklist(
                                request.getRefreshToken(),
                                jwtTokenProvider.getRefreshExpirationInMs()
                        );
                }

                // Update user status if needed
                user.setStatus("offline");
                userRepository.save(user);

                return ApiResponse.<Void>builder()
                        .status(HttpStatus.OK.value())
                        .timestamp(LocalDateTime.now())
                        .reason(HttpStatus.OK.name())
                        .message("Logout successful")
                        .path("api/auth/logout")
                        .build();
        }

        @Override
        public ApiResponse<Boolean> checkUserExistsById(UUID userId) {
            boolean exists = userRepository.existsById(userId);
            return ApiResponse.<Boolean>builder()
                    .data(exists)
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.OK.value())
                    .reason(HttpStatus.OK.name())
                    .path("api/auth/check-user-exists")
                    .message("User existence check completed")
                    .build();
        }
}