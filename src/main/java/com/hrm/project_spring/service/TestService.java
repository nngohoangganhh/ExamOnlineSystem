package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.question.TestSummaryResponse;
import com.hrm.project_spring.dto.test.TestRequest;
import com.hrm.project_spring.dto.test.TestResponse;
import com.hrm.project_spring.dto.test.TestScheduleRequest;
import com.hrm.project_spring.entity.*;
import com.hrm.project_spring.enums.AuditAction;
import com.hrm.project_spring.enums.ExamStatus;
import com.hrm.project_spring.enums.TestStatus;
import com.hrm.project_spring.exception.BadRequestException;
import com.hrm.project_spring.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final AuditLogService auditLogService;


    @Transactional(readOnly = true)
    public PageResponse<TestSummaryResponse> getAllTest(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Test> page = testRepository.findAll(pageable);
        List<TestSummaryResponse> data = page.getContent()
                .stream()
                .map(this::mapToSummary)
                .toList();
        return PageResponse.<TestSummaryResponse>builder()
                .content(data)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public TestResponse getTestById(Long id) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ""));
        return mapToResponse(test);
    }


    @Transactional
    public TestResponse createTest(TestRequest request, HttpServletRequest httpRequest) {
        if (request.getExamId() == null) {
            throw new BadRequestException("");
        }
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ""));

        if (exam.getStatus() == ExamStatus.ARCHIVED) {
            throw new BadRequestException(".");
        }

        validateCooldown(request);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ""));

        BigDecimal passingScore = request.getPassingScore() != null
                ? request.getPassingScore()
                : request.getTotalScore().multiply(BigDecimal.valueOf(0.5));

        Test test = Test.builder()
                .title(request.getTitle())
                .type(request.getType() != null ? request.getType().name() : null)
                .durationMinutes(request.getDurationMinutes())
                .totalScore(request.getTotalScore())
                .passingScore(passingScore)
                .maxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 1)
                .cooldownMinutes(request.getCooldownMinutes() != null ? request.getCooldownMinutes() : 0)
                .scoringPolicy(request.getScoringPolicy())
                .shuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()))
                .shuffleOptions(Boolean.TRUE.equals(request.getShuffleOptions()))
                .showResultImmediately(Boolean.TRUE.equals(request.getShowResultImmediately()))
                .allowReviewAfterSubmit(!Boolean.FALSE.equals(request.getAllowReviewAfterSubmit()))
                .showCorrectAnswers(Boolean.TRUE.equals(request.getShowCorrectAnswers()))
                .antiCheatConfig(request.getAntiCheatConfig())
                .openTime(request.getOpenTime())
                .closeTime(request.getCloseTime())
                .status(TestStatus.DRAFT)
                .exam(exam)
                .createdBy(user)
                .build();

        Test saved = testRepository.save(test);

        auditLogService.log(user.getId(), username, AuditAction.TEST_CREATE, httpRequest,
                "{\"testId\":" + saved.getId() + ",\"examId\":" + exam.getId() + "}");

        return mapToResponse(saved);
    }


    @Transactional
    public TestResponse updateTest(Long id, TestRequest request, HttpServletRequest httpRequest) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ""));

        TestStatus currentStatus = test.getStatus();

        if (currentStatus == TestStatus.OPEN) {
            test.setOpenTime(request.getOpenTime());
            test.setCloseTime(request.getCloseTime());
            test.setUpdatedAt(LocalDateTime.now());
            Test saved = testRepository.save(test);
            auditLogService.log(null, SecurityContextHolder.getContext().getAuthentication().getName(),
                    AuditAction.TEST_UPDATE, httpRequest, "{\"testId\":" + id + ",\"action\":\"schedule_only\"}");
            return mapToResponse(saved);
        }

        if (currentStatus != TestStatus.DRAFT && currentStatus != TestStatus.READY) {
            throw new BadRequestException(" " + currentStatus + ".");
        }

        validateCooldown(request);

        if (request.getExamId() != null) {
            Exam exam = examRepository.findById(request.getExamId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ""));
            if (exam.getStatus() == ExamStatus.ARCHIVED) {
                throw new BadRequestException("");
            }
            test.setExam(exam);
        }

        BigDecimal passingScore = request.getPassingScore() != null
                ? request.getPassingScore()
                : request.getTotalScore() != null
                    ? request.getTotalScore().multiply(BigDecimal.valueOf(0.5))
                    : test.getPassingScore();

        test.setTitle(request.getTitle());
        if (request.getType() != null) test.setType(request.getType().name());
        test.setDurationMinutes(request.getDurationMinutes());
        test.setTotalScore(request.getTotalScore());
        test.setPassingScore(passingScore);
        if (request.getMaxAttempts() != null) test.setMaxAttempts(request.getMaxAttempts());
        if (request.getCooldownMinutes() != null) test.setCooldownMinutes(request.getCooldownMinutes());
        if (request.getScoringPolicy() != null) test.setScoringPolicy(request.getScoringPolicy());
        if (request.getShuffleQuestions() != null) test.setShuffleQuestions(request.getShuffleQuestions());
        if (request.getShuffleOptions() != null) test.setShuffleOptions(request.getShuffleOptions());
        if (request.getShowResultImmediately() != null) test.setShowResultImmediately(request.getShowResultImmediately());
        if (request.getAllowReviewAfterSubmit() != null) test.setAllowReviewAfterSubmit(request.getAllowReviewAfterSubmit());
        if (request.getShowCorrectAnswers() != null) test.setShowCorrectAnswers(request.getShowCorrectAnswers());
        if (request.getAntiCheatConfig() != null) test.setAntiCheatConfig(request.getAntiCheatConfig());
        test.setOpenTime(request.getOpenTime());
        test.setCloseTime(request.getCloseTime());
        test.setUpdatedAt(LocalDateTime.now());

        boolean hasQuestions = !testQuestionRepository.findAllByTestOrderByOrderNumAsc(test).isEmpty();
        boolean hasEnrollments = test.getEnrollments() != null && !test.getEnrollments().isEmpty();
        boolean hasSchedule = test.getOpenTime() != null;
        if (hasQuestions && hasEnrollments && hasSchedule && currentStatus == TestStatus.DRAFT) {
            test.setStatus(TestStatus.READY);
        }

        Test saved = testRepository.save(test);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log(null, username, AuditAction.TEST_UPDATE, httpRequest,
                "{\"testId\":" + id + "}");

        return mapToResponse(saved);
    }


    @Transactional
    public void deleteTest(Long id, HttpServletRequest httpRequest) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ""));
        test.setDeletedAt(LocalDateTime.now());
        testRepository.save(test);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log(null, username, AuditAction.TEST_DELETE, httpRequest,
                "{\"testId\":" + id + "}");
    }


    @Transactional
    public TestResponse schedule(Long testId, TestScheduleRequest request, HttpServletRequest httpRequest) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ""));

        if (test.getStatus() != TestStatus.READY && test.getStatus() != TestStatus.OPEN) {
            throw new BadRequestException("");
        }

        if (request.isOpenNow()) {

            test.setStatus(TestStatus.OPEN);
            test.setOpenTime(LocalDateTime.now());
            if (request.getCloseTime() != null) {
                test.setCloseTime(request.getCloseTime());
            }
        } else {
            if (request.getOpenTime() != null && request.getCloseTime() != null
                    && !request.getCloseTime().isAfter(request.getOpenTime())) {
                throw new BadRequestException("");
            }
            if (request.getOpenTime() != null && request.getCloseTime() != null
                    && request.getCloseTime().isAfter(request.getOpenTime().plusDays(30))) {
                throw new BadRequestException("");
            }
            test.setOpenTime(request.getOpenTime());
            test.setCloseTime(request.getCloseTime());
        }

        test.setUpdatedAt(LocalDateTime.now());
        Test saved = testRepository.save(test);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log(null, username, AuditAction.TEST_PUBLISH, httpRequest,
                "{\"testId\":" + testId + ",\"openNow\":" + request.isOpenNow() + "}");

        return mapToResponse(saved);
    }


    @Transactional
    public TestResponse closeNow(Long testId, HttpServletRequest httpRequest) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ""));

        if (test.getStatus() != TestStatus.OPEN) {
            throw new BadRequestException("");
        }

        test.setStatus(TestStatus.CLOSED);
        test.setCloseTime(LocalDateTime.now());
        test.setUpdatedAt(LocalDateTime.now());
        Test saved = testRepository.save(test);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log(null, username, AuditAction.TEST_CLOSE, httpRequest,
                "{\"testId\":" + testId + "}");

        return mapToResponse(saved);
    }


    @Transactional
    public TestResponse archive(Long testId, HttpServletRequest httpRequest) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ""));

        if (test.getStatus() == TestStatus.ARCHIVED) {
            throw new BadRequestException("");
        }

        test.setStatus(TestStatus.ARCHIVED);
        test.setUpdatedAt(LocalDateTime.now());
        Test saved = testRepository.save(test);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log(null, username, AuditAction.TEST_UPDATE, httpRequest,
                "{\"testId\":" + testId + ",\"action\":\"archive\"}");

        return mapToResponse(saved);
    }


    public TestResponse mapToResponse(Test test) {
        List<TestQuestion> tqs = testQuestionRepository.findAllByTestOrderByOrderNumAsc(test);
        List<TestResponse.QuestionDto> questionDtos = tqs.stream().map(tq -> {
            Question q = tq.getQuestion();
            List<TestResponse.AnswerDto> answerDtos = q.getQuestionOptions() == null ? List.of() :
                    q.getQuestionOptions().stream().map(a ->
                            TestResponse.AnswerDto.builder()
                            .id(a.getId())
                            .content(a.getContent())
                            .build()
                    ).toList();
            return TestResponse.QuestionDto.builder()
                    .id(q.getId())
                    .content(q.getStem())
                    .type(q.getType() != null ? q.getType().name() : null)
                    .bloomLevel(q.getBloomLevel())
                    .score(tq.getScore())
                    .orderNum(tq.getOrderNum())
                    .answers(answerDtos)
                    .build();
        }).toList();

        return TestResponse.builder()
                .id(test.getId())
                .examId(test.getExam() != null ? test.getExam().getId() : null)
                .title(test.getTitle())
                .status(test.getStatus())
                .durationMinutes(test.getDurationMinutes())
                .totalScore(test.getTotalScore())
                .passingScore(test.getPassingScore())
                .maxAttempts(test.getMaxAttempts())
                .scoringPolicy(test.getScoringPolicy())
                .shuffleQuestions(test.getShuffleQuestions())
                .shuffleOptions(test.getShuffleOptions())
                .openTime(test.getOpenTime())
                .closeTime(test.getCloseTime())
                .createdAt(test.getCreatedAt())
                .questions(questionDtos)
                .build();
    }

    private TestSummaryResponse mapToSummary(Test test) {
        return TestSummaryResponse.builder()
                .id(test.getId())
                .examId(test.getExam() != null ? test.getExam().getId() : null)
                .title(test.getTitle())
                .durationMinutes(test.getDurationMinutes())
                .totalScore(test.getTotalScore() != null ? test.getTotalScore().intValue() : 0)
                .createAt(test.getCreatedAt())
                .build();
    }


    private void validateCooldown(TestRequest request) {
        Integer maxAttempts = request.getMaxAttempts();
        if (maxAttempts != null && maxAttempts > 1) {
            if (request.getCooldownMinutes() == null) {
                throw new BadRequestException("");
            }
        }
    }
}
