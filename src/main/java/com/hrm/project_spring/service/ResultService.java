package com.hrm.project_spring.service;

import com.hrm.project_spring.entity.*;
import com.hrm.project_spring.enums.AttemptStatus;
import com.hrm.project_spring.exception.BadRequestException;
import com.hrm.project_spring.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * UC42–UC44, UC47: Báo cáo kết quả thi.
 * UC42 – Kết quả thí sinh theo bài thi (điểm, pass/fail)
 * UC43 – Thống kê bài thi (điểm TB, min, max, pass rate)
 * UC44 – Phân tích câu hỏi (tỉ lệ đúng từng câu)
 * UC47 – Lịch sử làm bài của thí sinh
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {

    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final TestQuestionRepository testQuestionRepository;

    // ─── UC42: Kết quả thí sinh cho 1 bài thi ────────────────────────────────

    /**
     * Trả về danh sách attempt đã submit của tất cả thí sinh trong 1 bài thi.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTestResults(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BadRequestException("Bài thi không tồn tại."));

        List<Attempt> attempts = attemptRepository
                .findAllByTestOrderByUserAscAttemptNumberAsc(test)
                .stream()
                .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .toList();

        List<Map<String, Object>> results = new ArrayList<>();
        for (Attempt attempt : attempts) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("attemptId", attempt.getId());
            row.put("userId", attempt.getUser().getId());
            row.put("username", attempt.getUser().getUsername());
            row.put("fullName", attempt.getUser().getFullName());
            row.put("attemptNumber", attempt.getAttemptNumber());
            row.put("finalScore", attempt.getFinalScore());
            row.put("totalScore", test.getTotalScore());
            row.put("passingScore", test.getPassingScore());
            boolean passed = test.getPassingScore() != null &&
                    attempt.getFinalScore() != null &&
                    attempt.getFinalScore().compareTo(test.getPassingScore()) >= 0;
            row.put("passed", passed);
            row.put("status", attempt.getStatus().name());
            row.put("startedAt", attempt.getStartedAt());
            row.put("submittedAt", attempt.getSubmittedAt());
            results.add(row);
        }
        return results;
    }

    // ─── UC43: Thống kê bài thi ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getTestStatistics(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BadRequestException("Bài thi không tồn tại."));

        List<Attempt> submitted = attemptRepository
                .findAllByTestOrderByUserAscAttemptNumberAsc(test)
                .stream()
                .filter(a -> a.getFinalScore() != null)
                .toList();

        long total = submitted.size();
        long passed = submitted.stream()
                .filter(a -> test.getPassingScore() != null &&
                        a.getFinalScore().compareTo(test.getPassingScore()) >= 0)
                .count();

        OptionalDouble avg = submitted.stream()
                .mapToDouble(a -> a.getFinalScore().doubleValue()).average();
        Optional<Attempt> maxAttempt = submitted.stream()
                .max(Comparator.comparing(Attempt::getFinalScore));
        Optional<Attempt> minAttempt = submitted.stream()
                .min(Comparator.comparing(Attempt::getFinalScore));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("testId", testId);
        stats.put("testTitle", test.getTitle());
        stats.put("totalAttempts", total);
        stats.put("passCount", passed);
        stats.put("passRate", total > 0
                ? BigDecimal.valueOf((double) passed / total * 100).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        stats.put("averageScore", avg.isPresent()
                ? BigDecimal.valueOf(avg.getAsDouble()).setScale(2, RoundingMode.HALF_UP)
                : null);
        stats.put("maxScore", maxAttempt.map(Attempt::getFinalScore).orElse(null));
        stats.put("minScore", minAttempt.map(Attempt::getFinalScore).orElse(null));
        return stats;
    }

    // ─── UC44: Phân tích từng câu hỏi ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQuestionAnalysis(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BadRequestException("Bài thi không tồn tại."));

        List<TestQuestion> testQuestions = testQuestionRepository
                .findAllByTestOrderByOrderNumAsc(test);

        List<Attempt> allAttempts = attemptRepository
                .findAllByTestOrderByUserAscAttemptNumberAsc(test)
                .stream()
                .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .toList();

        List<Map<String, Object>> analysis = new ArrayList<>();
        for (TestQuestion tq : testQuestions) {
            Question q = tq.getQuestion();
            long totalAnswers = 0;
            long correctAnswers = 0;
            for (Attempt attempt : allAttempts) {
                Optional<AttemptAnswer> aa = attemptAnswerRepository
                        .findByAttemptAndQuestion(attempt, q);
                if (aa.isPresent()) {
                    totalAnswers++;
                    if (Boolean.TRUE.equals(aa.get().getIsCorrect())) correctAnswers++;
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("questionId", q.getId());
            row.put("questionStem", q.getStem());
            row.put("type", q.getType().name());
            row.put("orderNum", tq.getOrderNum());
            row.put("score", tq.getScore());
            row.put("totalAnswers", totalAnswers);
            row.put("correctAnswers", correctAnswers);
            row.put("correctRate", totalAnswers > 0
                    ? BigDecimal.valueOf((double) correctAnswers / totalAnswers * 100)
                            .setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            analysis.add(row);
        }
        return analysis;
    }

    // ─── UC47: Lịch sử làm bài của thí sinh ─────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyHistory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng."));

        // Lấy tất cả attempt của user, đã submitted
        List<Attempt> myAttempts = attemptRepository.findAll().stream()
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(Attempt::getStartedAt).reversed())
                .toList();

        List<Map<String, Object>> history = new ArrayList<>();
        for (Attempt a : myAttempts) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("attemptId", a.getId());
            row.put("testId", a.getTest().getId());
            row.put("testTitle", a.getTest().getTitle());
            row.put("examName", a.getTest().getExam() != null ? a.getTest().getExam().getName() : null);
            row.put("attemptNumber", a.getAttemptNumber());
            row.put("finalScore", a.getFinalScore());
            row.put("totalScore", a.getTest().getTotalScore());
            row.put("status", a.getStatus().name());
            row.put("startedAt", a.getStartedAt());
            row.put("submittedAt", a.getSubmittedAt());
            history.add(row);
        }
        return history;
    }
}
