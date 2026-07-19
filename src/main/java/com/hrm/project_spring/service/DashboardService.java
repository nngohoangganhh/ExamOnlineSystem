package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.dashboard.AdminDashboardResponse;
import com.hrm.project_spring.dto.dashboard.StudentDashboardResponse;
import com.hrm.project_spring.dto.exam.MyExamResponse;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.repository.ExamAttemptRepository;
import com.hrm.project_spring.repository.ExamRepository;
import com.hrm.project_spring.repository.QuestionRepository;
import com.hrm.project_spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository attemptRepository;

    // ======================== ADMIN DASHBOARD ========================

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        long totalUsers = userRepository.count();
        long totalExams = examRepository.count();
        long totalQuestions = questionRepository.count();
        long totalAttempts = attemptRepository.count();
        long openExams = examRepository.countByStatus("OPEN");

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalExams(totalExams)
                .totalQuestions(totalQuestions)
                .totalAttempts(totalAttempts)
                .openExams(openExams)
                .build();
    }

    // ======================== STUDENT DASHBOARD ========================

    @Transactional(readOnly = true)
    public StudentDashboardResponse getStudentDashboard() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));

        long totalAttempts = attemptRepository.countByUserId(user.getId());
        long submittedAttempts = attemptRepository.countByUserIdAndSubmitTimeIsNotNull(user.getId());
        Double averageScore = attemptRepository.findAverageScoreByUserId(user.getId());
        long totalExamsAssigned = examRepository.findByStudentId(user.getId(), PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();

        return StudentDashboardResponse.builder()
                .totalAttempts(totalAttempts)
                .submittedAttempts(submittedAttempts)
                .averageScore(averageScore != null ? Math.round(averageScore * 100.0) / 100.0 : 0.0)
                .totalExamsAssigned(totalExamsAssigned)
                .build();
    }

    // ======================== MY EXAMS (for students) ========================

    @Transactional(readOnly = true)
    public List<MyExamResponse> getMyExams() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));

        Page<com.hrm.project_spring.entity.Exam> exams =
                examRepository.findByStudentId(user.getId(), PageRequest.of(0, 100));

        return exams.getContent().stream()
                .map(exam -> {
                    List<MyExamResponse.MyTestSummary> testSummaries = exam.getTests() == null
                            ? List.of()
                            : exam.getTests().stream()
                            .map(test -> {
                                boolean submitted = attemptRepository
                                        .existsByUserIdAndTestIdAndSubmitTimeIsNotNull(user.getId(), test.getId());
                                Long lastAttemptId = attemptRepository
                                        // Dùng findFirst để tránh NonUniqueResultException
                                        // khi student thi lại nhiều lần cùng 1 test
                                        .findFirstByUserIdAndTestIdOrderByIdDesc(user.getId(), test.getId())
                                        .map(a -> a.getId())
                                        .orElse(null);

                                return MyExamResponse.MyTestSummary.builder()
                                        .testId(test.getId())
                                        .testTitle(test.getTitle())
                                        .durationMinutes(test.getDurationMinutes())
                                        .totalScore(test.getTotalScore())
                                        .alreadySubmitted(submitted)
                                        .lastAttemptId(lastAttemptId)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return MyExamResponse.builder()
                            .examId(exam.getId())
                            .examName(exam.getName())
                            .examDescription(exam.getDescription())
                            .startTime(exam.getStartTime())
                            .endTime(exam.getEndTime())
                            .status(exam.getStatus())
                            .tests(testSummaries)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
