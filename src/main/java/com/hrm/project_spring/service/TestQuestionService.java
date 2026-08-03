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

/**
 * UC29: Gán câu hỏi vào bài thi.
 * BR-048: Tối đa 200 câu hỏi/đề.
 * BR-049: Score scaling.
 * Chỉ gán câu hỏi APPROVED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestQuestionService {

    private static final int MAX_QUESTIONS_PER_TEST = 200;

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /**
     * UC29: Thêm câu hỏi vào bài thi (chọn tay theo ID).
     */
    @Transactional
    public void addQuestions(Long testId, List<Long> questionIds, HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng."));

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BadRequestException("Bài thi không tồn tại."));

        if (test.getStatus() != TestStatus.DRAFT) {
            throw new BadRequestException("Chỉ có thể sửa đề thi ở trạng thái DRAFT.");
        }

        // BR-048: Kiểm tra tổng số câu hỏi sau khi thêm
        int currentCount = testQuestionRepository.countByTest(test);
        if (currentCount + questionIds.size() > MAX_QUESTIONS_PER_TEST) {
            throw new BadRequestException(
                    "BR-048: Vượt quá số câu hỏi tối đa (" + MAX_QUESTIONS_PER_TEST + ").");
        }

        int orderStart = currentCount + 1;
        int idx = 0;
        for (Long questionId : questionIds) {
            // Chỉ gán câu hỏi APPROVED
            Question question = questionRepository.findActiveById(questionId)
                    .orElseThrow(() -> new BadRequestException(
                            "Câu hỏi " + questionId + " không tồn tại."));
            if (question.getStatus() != QuestionStatus.APPROVED) {
                throw new BadRequestException(
                        "Câu hỏi " + questionId + " chưa được duyệt (status: " + question.getStatus() + ").");
            }
            // Kiểm tra trùng
            if (testQuestionRepository.existsByTest_IdAndQuestion_Id(testId, questionId)) {
                log.warn("Câu hỏi {} đã tồn tại trong đề thi {}, bỏ qua.", questionId, testId);
                continue;
            }

            TestQuestion tq = TestQuestion.builder()
                    .test(test)
                    .question(question)
                    .orderNum(orderStart + idx)
                    .score(question.getScore())
                    .build();
            testQuestionRepository.save(tq);

            // Tăng usage_count
            question.setUsageCount(question.getUsageCount() + 1);
            questionRepository.save(question);
            idx++;
        }

        // Cập nhật total score
        updateTotalScore(test);
        testRepository.save(test);

        auditLogService.log(currentUser.getId(), username, AuditAction.TEST_UPDATE, request,
                "{\"testId\":" + testId + ",\"addedQuestions\":" + idx + "}");
    }

    /**
     * UC29: Xóa câu hỏi khỏi bài thi.
     */
    @Transactional
    public void removeQuestion(Long testId, Long questionId, HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng."));

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BadRequestException("Bài thi không tồn tại."));

        if (test.getStatus() != TestStatus.DRAFT) {
            throw new BadRequestException("Chỉ có thể sửa đề thi ở trạng thái DRAFT.");
        }

        List<TestQuestion> tqs = testQuestionRepository.findAllByTestOrderByOrderNumAsc(test);
        TestQuestion toRemove = tqs.stream()
                .filter(tq -> tq.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Câu hỏi không có trong đề thi."));

        testQuestionRepository.delete(toRemove);

        // Giảm usage_count
        Question question = toRemove.getQuestion();
        if (question.getUsageCount() != null && question.getUsageCount() > 0) {
            question.setUsageCount(question.getUsageCount() - 1);
            questionRepository.save(question);
        }

        // Reorder remaining
        List<TestQuestion> remaining = testQuestionRepository.findAllByTestOrderByOrderNumAsc(test);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setOrderNum(i + 1);
        }
        testQuestionRepository.saveAll(remaining);

        updateTotalScore(test);
        testRepository.save(test);

        auditLogService.log(currentUser.getId(), username, AuditAction.TEST_UPDATE, request,
                "{\"testId\":" + testId + ",\"removedQuestion\":" + questionId + "}");
    }

    /** UC29: Lấy danh sách câu hỏi của đề thi. */
    @Transactional
    public List<TestQuestion> getTestQuestions(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BadRequestException("Bài thi không tồn tại."));
        return testQuestionRepository.findAllByTestOrderByOrderNumAsc(test);
    }

    private void updateTotalScore(Test test) {
        List<TestQuestion> tqs = testQuestionRepository.findAllByTestOrderByOrderNumAsc(test);
        BigDecimal total = tqs.stream()
                .map(TestQuestion::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        test.setTotalScore(total);
    }
}
