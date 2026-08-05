package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.question.TestSummaryResponse;
import com.hrm.project_spring.dto.test.AssignQuestionsRequest;
import com.hrm.project_spring.dto.test.TestRequest;
import com.hrm.project_spring.dto.test.TestResponse;
import com.hrm.project_spring.entity.Exam;
import com.hrm.project_spring.entity.Question;
import com.hrm.project_spring.entity.Test;
import com.hrm.project_spring.entity.TestQuestion;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.repository.ExamRepository;
import com.hrm.project_spring.repository.QuestionRepository;
import com.hrm.project_spring.repository.TestQuestionRepository;
import com.hrm.project_spring.repository.TestRepository;
import com.hrm.project_spring.repository.UserRepository;
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
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title không được để trống");
        }
        if (request.getTotalScore() == null || request.getTotalScore() <= 0 || request.getTotalScore() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tổng điểm của bài thi sẽ không được quá 10 điểm");
        }
        if (request.getDurationMinutes() == null || request.getDurationMinutes() <= 0 || request.getDurationMinutes() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian làm bài của bài thi sẽ không được quá 180 phút");
        }
        Exam exam = null;
        if (request.getExamId() != null) {
            exam = examRepository.findById(request.getExamId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam not found"));
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
        
        Test test = Test.builder()
                .title(request.getTitle())
                .durationMinutes(request.getDurationMinutes())
                .totalScore(BigDecimal.valueOf(request.getTotalScore()))
                .exam(exam)
                .createdBy(user)
                .build();
        Test savedTest = testRepository.save(test);
        return mapToResponse(savedTest);
    }

    @Transactional
    public TestResponse updateTest(Long id, TestRequest request) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test not found"));

        if (request.getExamId() != null) {
            Exam exam = examRepository.findById(request.getExamId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam not found"));
            test.setExam(exam);
        }
        test.setTitle(request.getTitle());
        test.setDurationMinutes(request.getDurationMinutes());
        test.setTotalScore(BigDecimal.valueOf(request.getTotalScore()));
        Test updatedTest = testRepository.save(test);
        return mapToResponse(updatedTest);
    }

    @Transactional
    public void deleteTest(Long id) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test not found"));
        testRepository.delete(test);
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
                    q.getQuestionOptions().stream().map(a ->
                            TestResponse.AnswerDto.builder()
                                    .id(a.getId())
                                    .content(a.getContent())
                                    .build()
                    ).toList();
            return TestResponse.QuestionDto.builder()
                    .id(q.getId())
                    .content(q.getStem())
                    .difficulty(q.getReferenceAnswer())
                    .answers(answerDtos)
                    .build();
        }).toList();

        return TestResponse.builder()
                .id(test.getId())
                .examId(test.getExam() != null ? test.getExam().getId() : null)
                .title(test.getTitle())
                .durationMinutes(test.getDurationMinutes())
                .totalScore(test.getTotalScore() != null ? test.getTotalScore().intValue() : 0)
                .createAt(null)
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
}
