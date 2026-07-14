package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.test.AssignQuestionsRequest;
import com.hrm.project_spring.dto.test.TestRequest;
import com.hrm.project_spring.dto.test.TestResponse;
import com.hrm.project_spring.entity.Exam;
import com.hrm.project_spring.entity.Question;
import com.hrm.project_spring.entity.Test;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.repository.ExamRepository;
import com.hrm.project_spring.repository.QuestionRepository;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;

    public PageResponse<TestResponse> getAllTest(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Test> page = testRepository.findAll(pageable);
        List<TestResponse> data = page.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();
        return PageResponse.<TestResponse>builder()
                .content(data)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
    public TestResponse getTestById(Long id) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, " không tìm thấy id của bài test"));
        return mapToResponse(test);
    }
    public TestResponse createTest(TestRequest request) {

         if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title không được để trống");
        }
          if (request.getTotalScore() == null || request.getTotalScore() <= 0 || request.getTotalScore() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, " Tổng điểm của bài thi sẽ không được quá 10 điểm");
         }
          if (request.getDurationMinutes() == null || request.getDurationMinutes() <= 0 || request.getDurationMinutes() >180) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "  thời gian làm bài của bài thi sẽ không được quá 180 phút");
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
                .totalScore(request.getTotalScore())
                .exam(exam)
                .createdBy(user)
                .createAt(LocalDateTime.now())
                .build();
        Test savedTest = testRepository.save(test);
        return mapToResponse(savedTest);
    }
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
        test.setTotalScore(request.getTotalScore());
        Test updatedTest = testRepository.save(test);
        return mapToResponse(updatedTest);
    }
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
        // Kiểm tra tất cả questionId có tồn tại không
        if (questions.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Một hoặc nhiều questionId không tồn tại");
        }
        // Ghi đè toàn bộ danh sách câu hỏi
        test.setQuestions(new HashSet<>(questions));
        Test saved = testRepository.save(test);
        return mapToResponse(saved);
    }
    public TestResponse mapToResponse(Test test) {
        // Map questions + answers (không expose isCorrect)
        List<TestResponse.QuestionDto> questionDtos = test.getQuestions() == null ? List.of() :
            test.getQuestions().stream().map(q -> {
                List<TestResponse.AnswerDto> answerDtos = q.getAnswers() == null ? List.of() :
                    q.getAnswers().stream().map(a ->
                        TestResponse.AnswerDto.builder()
                            .id(a.getId())
                            .content(a.getContent())
                            .build()
                    ).toList();
                return TestResponse.QuestionDto.builder()
                    .id(q.getId())
                    .content(q.getContent())
                    .difficulty(q.getDifficulty())
                    .answers(answerDtos)
                    .build();
            }).toList();

        return TestResponse.builder()
                .id(test.getId())
                .examId(test.getExam() != null ? test.getExam().getId() : null)
                .title(test.getTitle())
                .durationMinutes(test.getDurationMinutes())
                .totalScore(test.getTotalScore())
                .createAt(test.getCreateAt() != null ? LocalTime.from(test.getCreateAt()) : null)
                .questions(questionDtos)
                .build();
    }
}
