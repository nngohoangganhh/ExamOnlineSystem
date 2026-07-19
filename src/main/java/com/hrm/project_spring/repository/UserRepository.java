package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByResetPasswordToken(String token);

    Optional<User> findByUsernameOrEmail(String usernameOrEmail, String usernameOrEmail1);

    // ===== UC08: Kiểm tra unique bao gồm cả user soft deleted (BR-017) =====

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email")
    boolean existsByEmailIncludingDeleted(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username")
    boolean existsByUsernameIncludingDeleted(@Param("username") String username);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.studentCode = :code")
    boolean existsByStudentCode(@Param("code") String code);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.employeeCode = :code")
    boolean existsByEmployeeCode(@Param("code") String code);

    // ===== Activation token =====

    Optional<User> findByActivationToken(String activationToken);

    boolean existsByUsernameAndIdNot(@NotNull(message = "username không đuợc để trống ") String username, Long id);

    boolean existsByEmailAndIdNot(@NotBlank(message = "Email không được trống") @Email(message = "Email không đúng định dạng") String email, Long id);

    boolean existsByStudentCodeAndIdNot(String studentCode, Long id);

    boolean existsByEmployeeCodeAndIdNot(String employeeCode, Long id);
}

