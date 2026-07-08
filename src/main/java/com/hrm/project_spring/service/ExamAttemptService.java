package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.exam.ExamAttemptStart;
import com.hrm.project_spring.dto.exam.ExamAttemptSubmit;
import com.hrm.project_spring.dto.result.*;
import com.hrm.project_spring.entity.*;
import com.hrm.project_spring.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamAttemptService {

    private final ExamAttemptRepository attemptRepository;
    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final StudentAnswerRepository studentAnswerRepository;

    @Transactional
    public ExamAttemptStart startAttempt(Long testId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, " tài khoản không tồn tại"));
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test không tồn tại"));

        ExamAttempt attempt = ExamAttempt.builder()
                .user(user)
                .test(test)
                .startTime(LocalDateTime.now())
                .build();

        attempt = attemptRepository.save(attempt);
        return maptoStart(attempt);
    }

    @Transactional
    public ExamAttemptSubmit submitAttempt(Long attemptId, String username, AttemptSubmitRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, " tài khoản không tồn tại"));

        ExamAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, " bài thi không tồn tại "));
        // không cho nộp quá 1 lần
        if (attempt.getSubmitTime() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, " bài thi đã được nộp");
        }
        int totalCorrect = 0;
        if (request.getAnswers() != null) {
            for (StudentAnswerSubmitRequest ansReq : request.getAnswers()) {
                Question question = questionRepository.findById(ansReq.getQuestionId())
                        .orElseThrow(
                                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Câu hỏi không tồn tại"));
                boolean isCorrect = false;
                Answer selectedAnswer = null;
                if (ansReq.getSelectedAnswerId() != null) {
                    selectedAnswer = answerRepository
                            .findByIdAndQuestionId(ansReq.getSelectedAnswerId(), question.getId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "câu trả lời cho câu hỏi không phù hợp"));
                    isCorrect = Boolean.TRUE.equals(selectedAnswer.getIsCorrect());
                    if (isCorrect) {
                        totalCorrect++;
                    }
                }
                StudentAnswer studentAnswer = StudentAnswer.builder()
                        .attempt(attempt)
                        .question(question)
                        .selectedAnswer(selectedAnswer)
                        .isCorrect(isCorrect)
                        .build();
                attempt.addStudentAnswer(studentAnswer);
            }
        }
        attempt.setSubmitTime(LocalDateTime.now());
        attempt.setTotalCorrect(totalCorrect);
        int totalQuestions = attempt.getTest().getQuestions().size();
        Integer testTotalScore = attempt.getTest().getTotalScore();
        if (testTotalScore == null)
            testTotalScore = 10;
        if (totalQuestions > 0) {
            double rawScore = ((double) totalCorrect / totalQuestions) * testTotalScore;
            attempt.setScore((double) Math.round(rawScore * 100) / 100);
        } else {
            attempt.setScore(0.0);
        }
        attempt = attemptRepository.save(attempt);
        return mapToResponse(attempt);
    }

    // ======================== NEW: XEM KẾT QUẢ CHI TIẾT ========================

    /**
     * Học sinh xem chi tiết kết quả + từng câu trả lời đúng/sai của mình
     */
    @Transactional(readOnly = true)
    public AttemptDetailResponse getAttemptDetail(Long attemptId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));

        ExamAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bài thi không tồn tại hoặc không thuộc về bạn"));

        return buildAttemptDetail(attempt);
    }

    /**
     * Admin xem chi tiết bất kỳ attempt
     */
    @Transactional(readOnly = true)
    public AttemptDetailResponse getAttemptDetailAdmin(Long attemptId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bài thi không tồn tại"));
        return buildAttemptDetail(attempt);
    }

    // ======================== NEW: LỊCH SỬ THI ========================

    /**
     * Học sinh xem lịch sử thi của bản thân (phân trang)
     */
    @Transactional(readOnly = true)
    public PageResponse<AttemptSummaryResponse> getMyAttempts(String username, int pageNo, int pageSize) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));

        Page<ExamAttempt> page = attemptRepository.findByUserId(user.getId(), PageRequest.of(pageNo, pageSize));
        return toPageResponse(page);
    }

    /**
     * Admin xem tất cả bài thi theo test
     */
    @Transactional(readOnly = true)
    public PageResponse<AttemptSummaryResponse> getAttemptsByTest(Long testId, int pageNo, int pageSize) {
        Page<ExamAttempt> page = attemptRepository.findByTestId(testId, PageRequest.of(pageNo, pageSize));
        return toPageResponse(page);
    }

    /**
     * Admin xem tất cả bài thi theo exam
     */
    @Transactional(readOnly = true)
    public PageResponse<AttemptSummaryResponse> getAttemptsByExam(Long examId, int pageNo, int pageSize) {
        Page<ExamAttempt> page = attemptRepository.findByExamId(examId, PageRequest.of(pageNo, pageSize));
        return toPageResponse(page);
    }

    // ======================== MAP HELPERS ========================

    private AttemptDetailResponse buildAttemptDetail(ExamAttempt attempt) {
        List<StudentAnswer> studentAnswers = studentAnswerRepository.findByAttemptIdWithDetails(attempt.getId());

        List<StudentAnswerDetailResponse> answerDetails = studentAnswers.stream()
                .map(sa -> {
                    Answer correctAnswer = sa.getQuestion().getAnswers().stream()
                            .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                            .findFirst()
                            .orElse(null);

                    return StudentAnswerDetailResponse.builder()
                            .questionId(sa.getQuestion().getId())
                            .questionContent(sa.getQuestion().getContent())
                            .selectedAnswerId(sa.getSelectedAnswer() != null ? sa.getSelectedAnswer().getId() : null)
                            .selectedAnswerContent(sa.getSelectedAnswer() != null ? sa.getSelectedAnswer().getContent() : null)
                            .correctAnswerId(correctAnswer != null ? correctAnswer.getId() : null)
                            .correctAnswerContent(correctAnswer != null ? correctAnswer.getContent() : null)
                            .isCorrect(sa.getIsCorrect())
                            .build();
                })
                .collect(Collectors.toList());

        return AttemptDetailResponse.builder()
                .id(attempt.getId())
                .userId(attempt.getUser().getId())
                .username(attempt.getUser().getUsername())
                .testId(attempt.getTest().getId())
                .testTitle(attempt.getTest().getTitle())
                .startTime(attempt.getStartTime())
                .submitTime(attempt.getSubmitTime())
                .score(attempt.getScore())
                .totalCorrect(attempt.getTotalCorrect())
                .totalQuestions(attempt.getTest().getQuestions().size())
                .answers(answerDetails)
                .build();
    }

    private PageResponse<AttemptSummaryResponse> toPageResponse(Page<ExamAttempt> page) {
        List<AttemptSummaryResponse> content = page.getContent().stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());

        return PageResponse.<AttemptSummaryResponse>builder()
                .content(content)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private ExamAttemptSubmit mapToResponse(ExamAttempt attempt) {
        ExamAttemptSubmit dto = new ExamAttemptSubmit();
        dto.setId(attempt.getId());
        dto.setUserId(attempt.getUser().getId());
        dto.setTestId(attempt.getTest().getId());
        dto.setStartTime(attempt.getStartTime());
        dto.setSubmitTime(attempt.getSubmitTime());
        dto.setScore(attempt.getScore());
        dto.setTotalCorrect(attempt.getTotalCorrect());
        return dto;
    }

    private ExamAttemptStart maptoStart(ExamAttempt attempt) {
        ExamAttemptStart start = new ExamAttemptStart();
        start.setId(attempt.getId());
        start.setTestId(attempt.getTest().getId());
        start.setUserId(attempt.getUser().getId());
        start.setStartTime(attempt.getStartTime());
        return start;
    }

    private AttemptSummaryResponse mapToSummary(ExamAttempt attempt) {
        return AttemptSummaryResponse.builder()
                .id(attempt.getId())
                .testId(attempt.getTest().getId())
                .testTitle(attempt.getTest().getTitle())
                .examId(attempt.getTest().getExam() != null ? attempt.getTest().getExam().getId() : null)
                .examName(attempt.getTest().getExam() != null ? attempt.getTest().getExam().getName() : null)
                .startTime(attempt.getStartTime())
                .submitTime(attempt.getSubmitTime())
                .score(attempt.getScore())
                .totalCorrect(attempt.getTotalCorrect())
                .totalQuestions(attempt.getTest().getQuestions().size())
                .submitted(attempt.getSubmitTime() != null)
                .build();
    }
}
