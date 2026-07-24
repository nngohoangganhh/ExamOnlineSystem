package com.hrm.project_spring.entity;

import com.hrm.project_spring.enums.QuestionStatus;
import com.hrm.project_spring.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "questions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;


    @Column(columnDefinition = "TEXT", nullable = false)
    private String stem;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;


    @Column(nullable = false)
    private Integer bloomLevel;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionStatus status;


    @Column(columnDefinition = "TEXT")
    private String explanation;


    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;


    // Essay
    @Column(columnDefinition = "TEXT")
    private String referenceAnswer;


    @Column(columnDefinition = "TEXT")
    private String rubric;


    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionOption> questionOptions = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "question_tags", joinColumns = @JoinColumn(name = "question_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();
    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


}