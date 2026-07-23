package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.PasswordHistory;
import com.hrm.project_spring.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    /**
     * BR-008: Lấy 3 mật khẩu gần nhất của user (theo thứ tự mới nhất trước).
     */
    List<PasswordHistory> findTop3ByUserOrderByCreatedAtDesc(User user);
}
