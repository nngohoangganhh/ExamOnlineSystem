package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.UserStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    /**
     * Lọc theo role code (ADMIN, STUDENT, TEACHER)
     */
    public static Specification<User> hasRole(String roleCode) {
        return (root, query, cb) -> {
            if (roleCode == null || roleCode.isEmpty()) return null;
            // JOIN bảng user_roles → roles, lọc theo code
            return cb.equal(
                    root.join("roles", JoinType.INNER).get("code"),
                    roleCode.toUpperCase()
            );
        };
    }

    /**
     * Lọc theo status (ACTIVE, LOCKED, INACTIVE, DELETED)
     */
    public static Specification<User> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isEmpty()) return null;
            return cb.equal(root.get("status"), UserStatus.valueOf(status));
        };
    }

    /**
     * Tìm kiếm theo keyword (trong username, email, fullName)
     */
    public static Specification<User> containsKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("username")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("fullName")), pattern)
            );
        };
    }

    /**
     * Loại bỏ user đã soft-delete (status != DELETED)
     */
    public static Specification<User> notDeleted() {
        return (root, query, cb) ->
                cb.notEqual(root.get("status"), UserStatus.DELETED);
    }
}