package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
}
