package com.hrm.project_spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@Getter
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(
            unique = true, nullable = false
    )
    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<Question> questions = new HashSet<>();

}