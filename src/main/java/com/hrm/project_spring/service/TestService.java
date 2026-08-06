package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.test.TestSummaryResponse;
import com.hrm.project_spring.dto.test.AssignQuestionsRequest;
import com.hrm.project_spring.dto.test.TestRequest;
import com.hrm.project_spring.dto.test.TestResponse;
import com.hrm.project_spring.entity.*;
import com.hrm.project_spring.enums.TestStatus;
import com.hrm.project_spring.repository.*;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final TestQuestionRepository testQuestionRepository;

    @Transactional
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

    @Transactional
    public TestResponse getTestById(Long id) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy id của bài test"));
        return mapToResponse(test);
    }

    @Transactional
    public TestResponse createTest(TestRequest request) {
        // examId bắt buộc theo UC27 (bài thi phải thuộc kỳ thi)
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kỳ thi không tồn tại"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        Test test = Test.builder()
                .title(request.getTitle())
                .durationMinutes(request.getDurationMinutes())
                .totalScore(request.getTotalScore() != null
                        ? BigDecimal.valueOf(request.getTotalScore())
                        : BigDecimal.ZERO)
                .exam(exam)
                .createdBy(user)
                .status(TestStatus.DRAFT)
                .build();

        return mapToResponse(testRepository.save(test));
    }

    @Transactional
    public TestResponse updateTest(Long id, TestRequest request) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bài thi không tồn tại"));

        // Cập nhật kỳ thi cha nếu có thay đổi
        if (request.getExamId() != null && !request.getExamId().equals(
                test.getExam() != null ? test.getExam().getId() : null)) {
            Exam exam = examRepository.findById(request.getExamId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kỳ thi không tồn tại"));
            test.setExam(exam);
        }

        test.setTitle(request.getTitle());
        test.setDurationMinutes(request.getDurationMinutes());
        if (request.getTotalScore() != null) {
            test.setTotalScore(BigDecimal.valueOf(request.getTotalScore()));
        }
        test.setUpdatedAt(java.time.LocalDateTime.now());

        return mapToResponse(testRepository.save(test));
    }

    @Transactional
    public void deleteTest(Long id) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bài thi không tồn tại"));
        // Soft delete: đặt deletedAt thay vì xóa vật lý
        test.setDeletedAt(java.time.LocalDateTime.now());
        testRepository.save(test);
    }

    @Transactional
    public TestResponse assignQuestions(Long testId, AssignQuestionsRequest request) {
        if (testId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "testId không hợp lệ");
        }
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));

        List<Long> ids = request.getQuestionIds() != null ? request.getQuestionIds() : List.of();
        List<Question> questions = questionRepository.findAllById(ids);
        if (questions.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Một hoặc nhiều questionId không tồn tại");
        }

        // Xóa các câu hỏi cũ trong đề thi
        testQuestionRepository.deleteAllByTestId(testId);

        // Gán các câu hỏi mới vào junction table
        List<TestQuestion> testQuestions = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            testQuestions.add(TestQuestion.builder()
                    .test(test)
                    .question(q)
                    .orderNum(i + 1)
                    .score(q.getScore())
                    .build());
        }
        testQuestionRepository.saveAll(testQuestions);

        return mapToResponse(test);
    }

    public TestResponse mapToResponse(Test test) {
        List<TestQuestion> tqs = testQuestionRepository.findAllByTestOrderByOrderNumAsc(test);
        List<TestResponse.QuestionDto> questionDtos = tqs.stream().map(tq -> {
            Question q = tq.getQuestion();
            List<TestResponse.AnswerDto> answerDtos = q.getQuestionOptions() == null ? List.of() :
                    q.getQuestionOptions().stream().map(opt ->
                            TestResponse.AnswerDto.builder()
                            .id(opt.getId())
                            .content(opt.getContent())
                                    // isCorrect KHÔNG được expose ra ngoài
                            .build()
                    ).toList();
            return TestResponse.QuestionDto.builder()
                    .id(q.getId())
                    .stem(q.getStem())
                    .type(q.getType() != null ? q.getType().name() : null)
                    .bloomLevel(q.getBloomLevel())
                    .orderNum(tq.getOrderNum())
                    .score(tq.getScore())
                    .answers(answerDtos)
                    .build();
        }).toList();

        return TestResponse.builder()
                .id(test.getId())
                .examId(test.getExam() != null ? test.getExam().getId() : null)
                .examName(test.getExam() != null ? test.getExam().getName() : null)
                .title(test.getTitle())
                .durationMinutes(test.getDurationMinutes())
                .totalScore(test.getTotalScore())
                .passingScore(test.getPassingScore())
                .status(test.getStatus())
                .maxAttempts(test.getMaxAttempts())
                .shuffleQuestions(test.getShuffleQuestions())
                .shuffleOptions(test.getShuffleOptions())
                .showResultImmediately(test.getShowResultImmediately())
                .allowReviewAfterSubmit(test.getAllowReviewAfterSubmit())
                .createdAt(test.getCreatedAt())
                .questions(questionDtos)
                .build();
    }

    private TestSummaryResponse mapToSummary(Test test) {
        return TestSummaryResponse.builder()
                .id(test.getId())
                .examId(test.getExam() != null ? test.getExam().getId() : null)
                .examName(test.getExam() != null ? test.getExam().getName() : null)
                .title(test.getTitle())
                .durationMinutes(test.getDurationMinutes())
                .totalScore(test.getTotalScore())
                .status(test.getStatus())
                .createdAt(test.getCreatedAt())
                .build();
    }
}
