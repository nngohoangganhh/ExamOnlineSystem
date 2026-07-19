package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {
    Optional<ClassRoom> findByCode(String code);
    boolean existsByCode(String code);
}
