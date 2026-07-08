package com.hrm.project_spring.dto.auth;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
public class RefreshTokenRequest {
    @NotBlank(message = "RefreshToken không được để trống")
    private String refreshToken;
}
