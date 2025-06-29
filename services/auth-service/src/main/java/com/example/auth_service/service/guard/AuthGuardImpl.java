package com.example.auth_service.service.guard;

import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGuardImpl implements AuthGuard {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void checkRegistrationEligibility(RegisterRequest request) {
        if(userRepository.existsByEmail(request.getEmail())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

    }
}