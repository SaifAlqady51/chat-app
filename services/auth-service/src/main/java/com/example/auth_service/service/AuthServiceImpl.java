package com.example.auth_service.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RegisterRequest;
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

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is already in use");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .username(request.getEmail())
                .status("online")
                .build();

        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getJwtExpirationInMs()))
                .userId(user.getId())
                .username(user.getUsername())
                .status(user.getStatus())
                .email(user.getEmail())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> User.builder()
                        .passwordHash(passwordEncoder.encode("dummy"))
                        .build());

        boolean credentialsValid = passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()) && user.getId() != null;

        if (!credentialsValid) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password");
        }

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getJwtExpirationInMs()))
                .email(user.getEmail())
                .userId(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}