package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRepository extends JpaRepository<Test, Long> {
    boolean existsByTestQuestions_Question_Id(Long questionId);}
