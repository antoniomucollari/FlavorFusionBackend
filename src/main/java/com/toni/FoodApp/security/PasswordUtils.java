package com.toni.FoodApp.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordUtils {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateRandomPassword(int length) {
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
                .substring(0, length);
    }
}
