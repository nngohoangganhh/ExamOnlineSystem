package com.hrm.project_spring.repository;

import com.hrm.project_spring.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeatureRepository extends JpaRepository<Feature, Long> {
    Optional<Feature> findByCode(String code);

    boolean existsByCode(String code);
}
