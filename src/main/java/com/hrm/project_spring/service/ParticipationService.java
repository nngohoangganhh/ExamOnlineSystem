package com.hrm.project_spring.service;

import com.hrm.project_spring.entity.*;
import com.hrm.project_spring.enums.*;
import com.hrm.project_spring.exception.BadRequestException;
import com.hrm.project_spring.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * UC33–UC41: Toàn bộ flow tham gia thi của thí sinh.
 *
 * UC33 – Xem danh sách bài thi có enrollment + exam PUBLISHED
 * UC34 – Vào thi (tạo Attempt mới)
 * UC35 – Trả lời câu hỏi (upsert AttemptAnswer)
 * UC36 – Lưu tạm (auto-save)
 * UC37 – Chuyển câu (navigate, lưu answer hiện tại)
 * UC38 – Đánh dấu xem lại
 * UC39 – Nộp bài (validate, chấm MCQ/TF, tính điểm)
 * UC40 – Auto-submit (server-side, gọi bởi AutoSubmitScheduler)
 * UC41 – Xem lại bài thi (read-only)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final AuditLogService auditLogService;

    // ─── UC33: Xem danh sách bài thi ─────────────────────────────────────────

    /**
     * Trả về danh sách Test mà thí sinh có enrollment VÀ exam đang PUBLISHED.
     */
    @Transactional
    public List<Test> getAvailableTests() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = getUser(username);

        List<Enrollment> enrollments = enrollmentRepository.findAllByUser(user);
        return enrollments.stream()
                .map(Enrollment::getTest)
                .filter(test -> test.getDeletedAt() == null)
                .filter(test -> test.getExam() != null &&
                        test.getExam().getStatus() == ExamStatus.PUBLISHED)
                .filter(test -> test.getStatus() == TestStatus.OPEN)
                .toList();
    }

    // ─── UC34: Vào thi ────────────────────────────────────────────────────────

    /**
     * Tạo Attempt mới cho thí sinh.
     * Kiểm tra: enrollment tồn tại, bài thi đang OPEN, chưa vượt maxAttempts.
     */
    @Transactional
    public Attempt startAttempt(Long testId, HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = getUser(username);

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BadRequestException("Bài thi không tồn tại."));

        // Kiểm tra bài thi đang mở
        if (test.getStatus() != TestStatus.OPEN) {
            throw new BadRequestException("Bài thi chưa mở hoặc đã đóng.");
        }

        // Kiểm tra enrollment
        Enrollment enrollment = enrollmentRepository.findByTestAndUser(test, user)
                .orElseThrow(() -> new BadRequestException("Bạn không có quyền tham gia bài thi này."));

        // Kiểm tra không có attempt đang IN_PROGRESS
        attemptRepository.findByTestAndUserAndStatus(test, user, AttemptStatus.IN_PROGRESS)
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "Bạn đang có một lượt làm bài chưa hoàn thành (id=" + existing.getId() + ").");
                });

        // Kiểm tra maxAttempts
        if (test.getMaxAttempts() != null) {
            int used = attemptRepository.countByTestAndUser(test, user);
            if (used >= test.getMaxAttempts()) {
                throw new BadRequestException(
                        "Bạn đã hết lượt làm bài (" + test.getMaxAttempts() + " lượt).");
            }
        }

        // Kiểm tra closeTime
        if (test.getCloseTime() != null && LocalDateTime.now().isAfter(test.getCloseTime())) {
            throw new BadRequestException("Bài thi đã quá hạn nộp.");
        }

        int attemptNumber = attemptRepository.countByTestAndUser(test, user) + 1;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledEnd = now.plusMinutes(test.getDurationMinutes());

        // Trích xuất client info
        String clientIp = extractIp(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        Attempt attempt = Attempt.builder()
                .test(test)
                .user(user)
                .attemptNumber(attemptNumber)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(now)
                .scheduledEndAt(scheduledEnd)
                .clientIp(clientIp)
                .userAgent(userAgent != null && userAgent.length() > 500
                        ? userAgent.substring(0, 500) : userAgent)
                .build();

        Attempt saved = attemptRepository.save(attempt);

        // Tăng attemptsUsed trong enrollment
        enrollment.setAttemptsUsed(enrollment.getAttemptsUsed() + 1);
        enrollmentRepository.save(enrollment);

        auditLogService.log(user.getId(), username, AuditAction.ATTEMPT_START, request,
                "{\"testId\":" + testId + ",\"attemptId\":" + saved.getId() + "}");

        log.info("UC34: User {} started attempt {} for test {}", username, saved.getId(), testId);
        return saved;
    }

    // ─── UC35/UC36: Trả lời câu hỏi / lưu tạm ───────────────────────────────

    /**
     * UC35/UC36: Upsert câu trả lời.
     * BR-062: Client gọi mỗi 30s (auto-save) — server chỉ cần upsert.
     */
    @Transactional
    public AttemptAnswer saveAnswer(Long attemptId, Long questionId, String answerData) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = getUser(username);

        Attempt attempt = getActiveAttempt(attemptId, user);
        Question question = getQuestionInTest(attempt.getTest(), questionId);

        AttemptAnswer answer = attemptAnswerRepository
                .findByAttemptAndQuestion(attempt, question)
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();
        if (answer == null) {
            answer = AttemptAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .answerData(answerData)
                    .firstSavedAt(now)
                    .lastSavedAt(now)
                    .build();
        } else {
            answer.setAnswerData(answerData);
            answer.setLastSavedAt(now);
        }

        return attemptAnswerRepository.save(answer);
    }

    // ─── UC38: Đánh dấu xem lại ───────────────────────────────────────────────

    @Transactional
    public void toggleMarkForReview(Long attemptId, Long questionId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = getUser(username);

        Attempt attempt = getActiveAttempt(attemptId, user);
        Question question = getQuestionInTest(attempt.getTest(), questionId);

        AttemptAnswer answer = attemptAnswerRepository
                .findByAttemptAndQuestion(attempt, question)
                .orElseGet(() -> AttemptAnswer.builder()
                        .attempt(attempt).question(question)
                        .firstSavedAt(LocalDateTime.now())
                        .lastSavedAt(LocalDateTime.now())
                        .build());

        answer.setMarkedForReview(!Boolean.TRUE.equals(answer.getMarkedForReview()));
        attemptAnswerRepository.save(answer);
    }

    // ─── UC39: Nộp bài ───────────────────────────────────────────────────────

    /**
     * UC39: Thí sinh tự nộp bài.
     * Chấm tự động MCQ_SINGLE, MCQ_MULTIPLE, TRUE_FALSE.
     * ESSAY để null, chờ chấm tay.
     */
    @Transactional
    public Attempt submit(Long attemptId, HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = getUser(username);
        return doSubmit(attemptId, user, "MANUAL", request);
    }

    /**
     * UC40: Auto-submit (gọi bởi scheduler).
     * Không cần kiểm tra user hiện tại vì scheduler gọi.
     */
    @Transactional
    public Attempt autoSubmit(Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new BadRequestException("Attempt không tồn tại: " + attemptId));
        return doSubmit(attemptId, attempt.getUser(), "AUTO_SUBMIT", null);
    }

    private Attempt doSubmit(Long attemptId, User user, String submitReason,
                              HttpServletRequest request) {
        Attempt attempt = getActiveAttempt(attemptId, user);

        // Lấy tất cả câu trả lời
        List<AttemptAnswer> answers = attemptAnswerRepository.findAllByAttempt(attempt);

        // Lấy đề thi
        List<TestQuestion> testQuestions = testQuestionRepository
                .findAllByTestOrderByOrderNumAsc(attempt.getTest());

        // Chấm điểm tự động
        BigDecimal totalScore = BigDecimal.ZERO;
        for (AttemptAnswer answer : answers) {
            Question q = answer.getQuestion();
            if (q.getType() == QuestionType.ESSAY) {
                // Chờ chấm tay — không tính điểm ngay
                answer.setIsCorrect(null);
                answer.setScore(null);
            } else {
                boolean correct = gradeAutomatic(answer, q);
                answer.setIsCorrect(correct);
                // Lấy điểm từ TestQuestion
                BigDecimal qScore = testQuestions.stream()
                        .filter(tq -> tq.getQuestion().getId().equals(q.getId()))
                        .map(TestQuestion::getScore)
                        .findFirst()
                        .orElse(BigDecimal.ZERO);
                answer.setScore(correct ? qScore : BigDecimal.ZERO);
                if (correct) totalScore = totalScore.add(qScore);
            }
        }
        attemptAnswerRepository.saveAll(answers);

        // Cập nhật attempt
        attempt.setStatus("AUTO_SUBMIT".equals(submitReason)
                ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setSubmitReason(submitReason);
        attempt.setRawScore(totalScore);
        attempt.setFinalScore(totalScore); // Có thể override sau khi chấm tay

        Attempt saved = attemptRepository.save(attempt);

        AuditAction auditAction = "AUTO_SUBMIT".equals(submitReason)
                ? AuditAction.ATTEMPT_AUTO_SUBMIT : AuditAction.ATTEMPT_SUBMIT;
        auditLogService.log(user.getId(), user.getUsername(), auditAction, request,
                "{\"attemptId\":" + attemptId + ",\"score\":" + totalScore + "}");

        log.info("UC39/40: {} attempt {} — score={}", submitReason, attemptId, totalScore);
        return saved;
    }

    // ─── UC41: Xem lại bài thi ───────────────────────────────────────────────

    /**
     * UC41: Trả về danh sách câu trả lời của attempt (read-only).
     * Kiểm tra allowReviewAfterSubmit trước khi cho xem.
     */
    @Transactional
    public List<AttemptAnswer> reviewAttempt(Long attemptId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = getUser(username);

        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new BadRequestException("Attempt không tồn tại."));

        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Bạn không có quyền xem kết quả này.");
        }
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Bài thi chưa nộp.");
        }
        if (!Boolean.TRUE.equals(attempt.getTest().getAllowReviewAfterSubmit())) {
            throw new BadRequestException("Bài thi này không cho phép xem lại.");
        }

        return attemptAnswerRepository.findAllByAttemptOrderByIdAsc(attempt);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng."));
    }

    private Attempt getActiveAttempt(Long attemptId, User user) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new BadRequestException("Attempt không tồn tại."));
        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Bạn không có quyền truy cập attempt này.");
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Bài thi đã được nộp.");
        }
        return attempt;
    }

    private Question getQuestionInTest(Test test, Long questionId) {
        if (!testQuestionRepository.existsByTest_IdAndQuestion_Id(test.getId(), questionId)) {
            throw new BadRequestException("Câu hỏi không thuộc đề thi này.");
        }
        return testQuestionRepository.findAllByTestOrderByOrderNumAsc(test).stream()
                .filter(tq -> tq.getQuestion().getId().equals(questionId))
                .map(TestQuestion::getQuestion)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Câu hỏi không tồn tại."));
    }

    /**
     * Chấm điểm tự động cho MCQ_SINGLE, MCQ_MULTIPLE, TRUE_FALSE.
     * answerData = JSON string, so sánh với đáp án đúng trong QuestionOption.
     * Đơn giản hoá: so sánh answerData với selectedOptionId.
     */
    private boolean gradeAutomatic(AttemptAnswer answer, Question question) {
        if (answer.getAnswerData() == null || answer.getAnswerData().isBlank()) {
            return false;
        }
        try {
            // Đọc selectedOptionId từ answerData JSON
            String data = answer.getAnswerData();

            List<QuestionOption> correctOptions = question.getQuestionOptions().stream()
                    .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                    .toList();

            if (question.getType() == QuestionType.MCQ_SINGLE ||
                    question.getType() == QuestionType.TRUE_FALSE) {
                // {"selectedOptionId": 12}
                if (!data.contains("selectedOptionId")) return false;
                String idStr = data.replaceAll(".*\"selectedOptionId\"\\s*:\\s*(\\d+).*", "$1");
                long selectedId = Long.parseLong(idStr.trim());
                return correctOptions.stream()
                        .anyMatch(opt -> opt.getId() != null && opt.getId() == selectedId);
            } else if (question.getType() == QuestionType.MCQ_MULTIPLE) {
                // {"selectedOptionIds": [12, 13]}
                if (!data.contains("selectedOptionIds")) return false;
                String arrStr = data.replaceAll(".*\"selectedOptionIds\"\\s*:\\s*\\[([^\\]]+)\\].*", "$1");
                Set<Long> selectedIds = new java.util.HashSet<>();
                for (String s : arrStr.split(",")) {
                    selectedIds.add(Long.parseLong(s.trim()));
                }
                Set<Long> correctIds = correctOptions.stream()
                        .map(QuestionOption::getId)
                        .collect(java.util.stream.Collectors.toSet());
                return selectedIds.equals(correctIds);
            }
        } catch (Exception e) {
            log.warn("Lỗi chấm điểm tự động attemptAnswerId={}: {}", answer.getId(), e.getMessage());
        }
        return false;
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
