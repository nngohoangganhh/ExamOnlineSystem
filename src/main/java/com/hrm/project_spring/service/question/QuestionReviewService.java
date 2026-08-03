package com.hrm.project_spring.service.question;

import com.hrm.project_spring.entity.Question;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.AuditAction;
import com.hrm.project_spring.enums.QuestionStatus;
import com.hrm.project_spring.exception.BadRequestException;
import com.hrm.project_spring.repository.QuestionRepository;
import com.hrm.project_spring.repository.UserRepository;
import com.hrm.project_spring.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UC23: Review câu hỏi — approve / reject / bulkApprove.
 * BR-040: 4-eye rule — không được tự duyệt câu hỏi của chính mình.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionReviewService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /**
     * UC23: Duyệt câu hỏi.
     * BR-040: reviewer không được là người tạo câu hỏi.
     */
    @Transactional
    public void approve(Long questionId, HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User reviewer = getUser(username);
        Question question = getActiveQuestion(questionId);

        // BR-040: 4-eye rule
        if (question.getCreatedBy() != null &&
                question.getCreatedBy().getId().equals(reviewer.getId())) {
            throw new BadRequestException("BR-040: Không thể tự duyệt câu hỏi do chính mình tạo.");
        }

        if (question.getStatus() != QuestionStatus.PENDING_REVIEW) {
            throw new BadRequestException(
                    "Chỉ câu hỏi ở trạng thái PENDING_REVIEW mới có thể duyệt. " +
                    "Trạng thái hiện tại: " + question.getStatus());
        }

        question.setStatus(QuestionStatus.APPROVED);
        question.setApprover(reviewer);
        question.setApprovedAt(LocalDateTime.now());
        question.setRejectionComment(null);
        questionRepository.save(question);

        auditLogService.log(reviewer.getId(), username, AuditAction.QUESTION_APPROVE, request,
                "{\"questionId\":" + questionId + "}");
    }

    /**
     * UC23: Từ chối câu hỏi với lý do.
     * BR-040: reviewer không được là người tạo câu hỏi.
     */
    @Transactional
    public void reject(Long questionId, String reason, HttpServletRequest request) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Phải cung cấp lý do từ chối.");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User reviewer = getUser(username);
        Question question = getActiveQuestion(questionId);

        // BR-040: 4-eye rule
        if (question.getCreatedBy() != null &&
                question.getCreatedBy().getId().equals(reviewer.getId())) {
            throw new BadRequestException("BR-040: Không thể tự từ chối câu hỏi do chính mình tạo.");
        }

        if (question.getStatus() != QuestionStatus.PENDING_REVIEW) {
            throw new BadRequestException(
                    "Chỉ câu hỏi ở trạng thái PENDING_REVIEW mới có thể từ chối.");
        }

        question.setStatus(QuestionStatus.REJECTED);
        question.setRejectionComment(reason);
        question.setApprover(reviewer);
        question.setApprovedAt(null);
        questionRepository.save(question);

        auditLogService.log(reviewer.getId(), username, AuditAction.QUESTION_REJECT, request,
                "{\"questionId\":" + questionId + ",\"reason\":\"" + reason + "\"}");
    }

    /**
     * UC23: Duyệt hàng loạt câu hỏi.
     * BR-040: Lọc bỏ các câu hỏi do chính reviewer tạo.
     *
     * @return Số câu được duyệt thành công
     */
    @Transactional
    public int bulkApprove(List<Long> questionIds, HttpServletRequest request) {
        if (questionIds == null || questionIds.isEmpty()) return 0;

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User reviewer = getUser(username);

        List<Question> questions = questionRepository.findAllByStatusAndIdIn(
                QuestionStatus.PENDING_REVIEW, questionIds);

        int approved = 0;
        for (Question question : questions) {
            // BR-040: bỏ qua câu hỏi do chính mình tạo
            if (question.getCreatedBy() != null &&
                    question.getCreatedBy().getId().equals(reviewer.getId())) {
                log.warn("UC23 BR-040: Bỏ qua câu hỏi {} — tự duyệt.", question.getId());
                continue;
            }
            question.setStatus(QuestionStatus.APPROVED);
            question.setApprover(reviewer);
            question.setApprovedAt(LocalDateTime.now());
            question.setRejectionComment(null);
            approved++;
        }

        questionRepository.saveAll(questions);

        auditLogService.log(reviewer.getId(), username, AuditAction.QUESTION_APPROVE, request,
                "{\"bulkApproved\":" + approved + ",\"requested\":" + questionIds.size() + "}");

        return approved;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng."));
    }

    private Question getActiveQuestion(Long questionId) {
        return questionRepository.findActiveById(questionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Câu hỏi không tồn tại hoặc đã bị xóa."));
    }
}
