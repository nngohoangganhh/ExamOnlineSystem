package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByName(String name);
    Optional<Role> findByCode(String code);
    boolean existsByCode(String code);

    /**
     * Đếm số user cho TỪNG role bằng 1 query duy nhất.
     * Tránh N+1 query khi gọi role.getUsers().size() trong vòng lặp.
     *
     * Trả về List<Object[]> với mỗi phần tử là [roleId (Long), userCount (Long)].
     * Dùng LEFT JOIN để role chưa có user vẫn trả về count = 0.
     */
    @Query("SELECT r.id, COUNT(u) FROM Role r LEFT JOIN r.users u GROUP BY r.id")
    List<Object[]> countUsersByRole();
}
