package com.example.auth_service.service;

import java.time.Instant;

import com.example.auth_service.repository.TokenRepository;
import com.example.auth_service.service.validator.AuthValidator;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RefreshTokenRequest;
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
        private final TokenRepository tokenRepository;
        private final AuthValidator authValidator;

        public AuthServiceImpl(
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider,
                        TokenRepository tokenRepository,
                        AuthValidator authValidator
        ) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtTokenProvider = jwtTokenProvider;
                this.tokenRepository = tokenRepository;
                this.authValidator = authValidator;
        }

        @Override
        public AuthResponse register(RegisterRequest request) {
                try {
                        authValidator.validateRegisterRequest(request);
                        User user = User.builder()
                                .email(request.getEmail())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .username(request.getUsername())
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
                catch(DataAccessException e){
                        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Service temporarily unavailable. Please try again later.");

                }
        }

        @Override
        public AuthResponse login(LoginRequest request) {
                try {
                        authValidator.validateLoginRequest(request);
                        User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Invalid email or password"));

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

                        tokenRepository.saveRefreshToken(
                                user.getEmail(),
                                refreshToken,
                                jwtTokenProvider.getRefreshExpirationInMs()
                        );


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
                catch (ResponseStatusException e) {
                        // Re-throw the validation exceptions directly
                        throw e;
                }
                catch (DataAccessException e) {
                        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Service temporarily unavailable. Please try again later.");
                } catch (Exception e) {
                        log.error("Unexpected error during login", e);
                        throw new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "An unexpected error occurred");
                }
        }

        @Override
        public AuthResponse refreshToken(RefreshTokenRequest request) {
                try {
                        authValidator.validateRefreshTokenRequest(request);
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

                        return AuthResponse.builder()
                                .token(newAccessToken)
                                .refreshToken(newRefreshToken)
                                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getJwtExpirationInMs()))
                                .email(user.getEmail())
                                .userId(user.getId())
                                .username(user.getUsername())
                                .status(user.getStatus())
                                .build();
                }
                catch (DataAccessException e) {
                        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Service temporarily unavailable. Please try again later.");
                } catch (ResponseStatusException e) {
                        throw e;
                } catch (Exception e) {
                        log.error("Unexpected error during token refresh", e);
                        throw new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "An unexpected error occurred");
                }
        }
}