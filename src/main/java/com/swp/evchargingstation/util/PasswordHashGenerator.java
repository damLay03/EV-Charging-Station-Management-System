package com.swp.evchargingstation.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hash
 * Used for creating SQL insert scripts with encrypted passwords
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String password = "123456";

        // Generate 3 different hashes (BCrypt always produces different hashes for the same input)
        System.out.println("=== BCrypt Password Hashes for: " + password + " ===");
        System.out.println("Hash 1: " + encoder.encode(password));
        System.out.println("Hash 2: " + encoder.encode(password));
        System.out.println("Hash 3: " + encoder.encode(password));
        System.out.println("\nNote: All these hashes are valid for password '" + password + "'");
        System.out.println("BCrypt generates different hashes each time due to random salt.");
        System.out.println("\nFor SQL insert, you can use any of these hashes.");
    }
}

