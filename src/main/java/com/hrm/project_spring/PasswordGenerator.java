package com.hrm.project_spring;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "teacher123";
        String hashPassword = encoder.encode(rawPassword);

        System.out.println("Mật khẩu mới: " + rawPassword);
        System.out.println("BCrypt hash: " + hashPassword);

        System.out.println(
                "Kiểm tra: " + encoder.matches(rawPassword, hashPassword)
        );
    }
}