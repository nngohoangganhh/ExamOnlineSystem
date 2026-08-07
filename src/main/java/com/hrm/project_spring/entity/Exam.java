package com.hrm.project_spring.entity;

import com.hrm.project_spring.enums.ExamStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SRS v1.0 §11.13: Kỳ thi (UC25–UC26).
 * BUG FIX: createdAt từ LocalTime → LocalDateTime.
 * THÊM: code, semester, academicYear, startDate, endDate, owner, status enum.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UC25: Mã kỳ thi (duy nhất). VD: HK1-2024-2025 */
    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Học kỳ. VD: "HK1", "HK2". */
    @Column(length = 20)
    private String semester;

    /** Năm học. VD: "2024-2025". */
    @Column(name = "academic_year", length = 20)
    private String academicYear;

    /** Ngày bắt đầu kỳ thi (không phải giờ). */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** Ngày kết thúc kỳ thi (không phải giờ). */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Người tạo kỳ thi. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** Người phụ trách kỳ thi (có thể khác người tạo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExamStatus status = ExamStatus.DRAFT;


    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** UC26: Soft delete. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * UC25: Môn học liên kết với kỳ thi (bắt buộc).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    /** 1 kỳ thi có nhiều đề thi. */
    @OneToMany(mappedBy = "exam", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Test> tests = new ArrayList<>();

    /** Danh sách thí sinh tham gia kỳ thi. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "exam_students",
            joinColumns = @JoinColumn(name = "exam_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @Builder.Default
    private Set<User> students = new HashSet<>();
}
