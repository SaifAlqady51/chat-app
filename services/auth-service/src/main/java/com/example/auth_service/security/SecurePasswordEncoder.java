package com.example.auth_service.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SecurePasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public SecurePasswordEncoder() {
        this.bCryptPasswordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return bCryptPasswordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return bCryptPasswordEncoder.matches(rawPassword, encodedPassword);
    }

    public boolean upgradeEncoding(String encodedPassword) {
        return bCryptPasswordEncoder.upgradeEncoding(encodedPassword);
    }

    // Additional utility methods
    public static String generateRandomPassword() {
        // Implement secure random password generation if needed
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
