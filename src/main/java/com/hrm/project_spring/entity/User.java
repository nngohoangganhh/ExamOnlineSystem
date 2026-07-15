package com.hrm.project_spring.entity;

import com.hrm.project_spring.enums.Gender;
import com.hrm.project_spring.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    private String fullName;

    // ===== Trường mới theo SRS UC08 =====

    @Column(length = 15)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "student_code", length = 20)
    private String studentCode;

    @Column(name = "employee_code", length = 20)
    private String employeeCode;

    // ===== Activation token cho luồng kích hoạt email =====

    @Column(name = "activation_token", length = 64)
    private String activationToken;

    @Column(name = "activation_token_expiry")
    private LocalDateTime activationTokenExpiry;

    // ===== Status & timestamps =====

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ===== Lock =====

    @Column(name = "lock_reason", length = 500)
    private String lockReason;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_expiry")
    private LocalDateTime resetPasswordExpiry;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Builder.Default
    @Column(name = "failed_login_count", nullable = false)
    private Integer failedLoginCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Builder.Default
    @Column(name = "password_changed_at", nullable = false)
    private LocalDateTime passwordChangedAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "require_password_change", nullable = false)
    private Boolean requirePasswordChange = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @ManyToMany(mappedBy = "students", fetch = FetchType.LAZY)
    private Set<ClassRoom> classRooms = new HashSet<>();


}
