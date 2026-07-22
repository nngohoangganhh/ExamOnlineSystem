package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByStudentCodeAndIdNot(String studentCode, Long id);

    boolean existsByEmployeeCodeAndIdNot(String employeeCode, Long id);

    // ===== UC14: Tìm kiếm / lọc user =====

    /**
     * Tìm kiếm user với nhiều tiêu chí kết hợp (UC14).
     * keyword: tìm trong fullName, email, username, studentCode, employeeCode.
     * Các filter tùy chọn: status, roleId, classId, createdFrom, createdTo.
     * includeDeleted: nếu false → loại bỏ user có status=DELETED.
     */
    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN u.roles r
        LEFT JOIN u.classRooms c
        WHERE (
            :keyword IS NULL OR :keyword = '' OR
            LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(u.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(u.employeeCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (:status IS NULL OR u.status = :status)
        AND (:roleId IS NULL OR r.id = :roleId)
        AND (:classId IS NULL OR c.id = :classId)
        AND (:includeDeleted = true OR u.status <> com.hrm.project_spring.enums.UserStatus.DELETED)
        AND (:createdFrom IS NULL OR u.createdAt >= :createdFrom)
        AND (:createdTo IS NULL OR u.createdAt <= :createdTo)
    """)
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            @Param("roleId") Long roleId,
            @Param("classId") Long classId,
            @Param("includeDeleted") boolean includeDeleted,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            Pageable pageable
    );

    /**
     * UC11 BR-023: Tìm các user đã soft-deleted quá 30 ngày để purge.
     */
    @Query("SELECT u FROM User u WHERE u.status = com.hrm.project_spring.enums.UserStatus.DELETED AND u.deletedAt IS NOT NULL AND u.deletedAt < :threshold")
    java.util.List<User> findSoftDeletedBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * UC11 Export UC13: Tìm users với filter.
     */
    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN u.roles r
        LEFT JOIN u.classRooms c
        WHERE (:status IS NULL OR u.status = :status)
        AND (:roleId IS NULL OR r.id = :roleId)
        AND (:includeDeleted = true OR u.status <> com.hrm.project_spring.enums.UserStatus.DELETED)
        ORDER BY u.createdAt DESC
    """)
    java.util.List<User> findAllForExport(
            @Param("status") UserStatus status,
            @Param("roleId") Long roleId,
            @Param("includeDeleted") boolean includeDeleted
    );
}
