package com.hrm.project_spring.service.question;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.question.*;
import com.hrm.project_spring.entity.*;
import com.hrm.project_spring.enums.AuditAction;
import com.hrm.project_spring.enums.QuestionAction;
import com.hrm.project_spring.enums.QuestionStatus;
import com.hrm.project_spring.enums.QuestionType;
import com.hrm.project_spring.exception.BadRequestException;
import com.hrm.project_spring.mapper.QuestionMapper;
import com.hrm.project_spring.repository.*;
import com.hrm.project_spring.service.AuditLogService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ChapterRepository chapterRepository;
    private final SubjectRepository subjectRepository;
    private final TestRepository testRepository;
    private final TagRepository tagRepository;
    private final AuditLogService auditLogService;
    private final TestQuestionRepository testQuestionRepository;

    //  1. Lấy tất cả câu hỏi (phân trang) — chỉ câu hỏi chưa bị xóa
//    @Transactional
//    public PageResponse<QuestionResponse> getAllQuestion(int pageNo, int pageSize) {
//        Specification<Question> spec = QuestionSpecification.of(
//                null, null, null, null, null, null, null);
//        Page<Question> page = questionRepository.findAll(spec, PageRequest.of(pageNo, pageSize));
//        List<QuestionResponse> data = page.getContent()
//                .stream()
//                .map(QuestionMapper::toResponse)
//                .toList();
//        return PageResponse.<QuestionResponse>builder()
//                .content(data)
//                .pageNo(page.getNumber())
//                .pageSize(page.getSize())
//                .totalElements(page.getTotalElements())
//                .totalPages(page.getTotalPages())
//                .last(page.isLast())
//                .build();
//    }

    // UC24: Tìm kiếm câu hỏi theo nhiều tiêu chí
    @Transactional
    public PageResponse<QuestionResponse> search(Long subjectId, Long chapterId,
                                                  Integer bloomLevel, QuestionStatus status,
                                                  String keyword, String tag,
                                                  int pageNo, int pageSize) {
        Specification<Question> spec = QuestionSpecification.of(
                subjectId, chapterId, bloomLevel, status, keyword, tag, null);
        Page<Question> page = questionRepository.findAll(spec, PageRequest.of(pageNo, pageSize));
        List<QuestionResponse> data = page.getContent().stream()
                .map(QuestionMapper::toResponse).toList();
        return PageResponse.<QuestionResponse>builder()
                .content(data)
                .pageNo(page.getNumber()).pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // 2. Lấy câu hỏi theo id
    @Transactional
    public QuestionDetailResponse getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy câu hỏi"));
        return QuestionMapper.toMapperResponse(question);
    }

    @Transactional
    public QuestionResponse create(CreateQuestionRequest request) {
        // ==========================
        // 1. Validate nghiệp vụ
        // ==========================
        // Validate stem sau khi bỏ HTML
        String plainText = Jsoup.parse(request.getStem()).text().trim();
        if (plainText.length() < 10 ) {
            throw new BadRequestException("Nội dung câu hỏi quá ngắn.");
        }
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new BadRequestException("Môn học không tồn tại."));
        // Validate Chapter
        Chapter chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new BadRequestException("Chương không tồn tại."));
        // Chapter phải thuộc Subject
        if (!chapter.getSubject().getId().equals(subject.getId())) {
            throw new BadRequestException("Chương không thuộc môn học đã chọn.");
        }
        // Validate theo loại câu hỏi
        //câu hỏi 1 đáp án đúng
        if (request.getType() == QuestionType.MCQ_SINGLE) {
            if (request.getOptions() == null || request.getOptions().isEmpty()) {
                throw new BadRequestException("Phương án không hợp lệ ");
            }
            if (request.getOptions().size() < 2 || request.getOptions().size() > 8) {
                throw new BadRequestException("Số phương án từ 2 đến 8.");
            }
            long correctCount = request.getOptions()
                    .stream()
                    .filter(QuestionOptionRequest::getIsCorrect)
                    .count();
            if (correctCount == 0) {
                throw new BadRequestException("Phải chọn 1 đáp án đúng.");
            }
            if (correctCount > 1) {
                throw new BadRequestException("Loại MCQ-Single chỉ cho phép 1 đáp án đúng.");
            }
        }
        // câu hỏi nhiều đáp án đúng
        if (request.getType() == QuestionType.MCQ_MULTIPLE) {
            long correctCount = request.getOptions()
                    .stream()
                    .filter(QuestionOptionRequest::getIsCorrect)
                    .count();

            if (correctCount == 0) {
                throw new BadRequestException("MCQ-Multiple phải có ít nhất 1 đáp án đúng.");
            }
        }
        // tự luận ngắn
        if (request.getType() == QuestionType.ESSAY) {

            if (request.getOptions() != null
                    && !request.getOptions().isEmpty()) {
                throw new BadRequestException(
                        "Câu hỏi tự luận không có phương án trả lời"
                );
            }
            if (request.getReferenceAnswer() == null) {
                throw new BadRequestException(
                        "Tự luận cần có đáp án tham khảo"
                );
            }
        }

        // câu hỏi đúng sai
        if (request.getType() == QuestionType.TRUE_FALSE) {
            if (request.getOptions() == null
                    || request.getOptions().size() != 2) {
                throw new BadRequestException(
                        "Câu hỏi Đúng/Sai phải có 2 phương án."
                );
            }
            long correctCount = request.getOptions()
                    .stream()
                    .filter(QuestionOptionRequest::getIsCorrect)
                    .count();
            if (correctCount != 1) {
                throw new BadRequestException(
                        "Câu hỏi Đúng/Sai phải có đúng 1 đáp án đúng."
                );
            }
            List<String> values = request.getOptions()
                    .stream()
                    .map(o -> o.getContent().trim().toLowerCase())
                    .toList();
            boolean valid =
                    values.contains("đúng")
                            && values.contains("sai");
            if (!valid) {
                throw new BadRequestException(
                        "Phương án của câu hỏi Đúng/Sai phải là Đúng và Sai."
                );
            }
        }
        // ==========================
        // 2. Lấy user hiện tại
        // ==========================
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng."));
        // ==========================
        // 3. Tạo Question
        // ==========================
        // UC16: status PENDING → PENDING_REVIEW (SRS v1.0)
        QuestionStatus initialStatus = (request.getAction() == QuestionAction.SUBMIT)
                ? QuestionStatus.PENDING_REVIEW
                : QuestionStatus.DRAFT;

        Question question = Question.builder()
                .stem(request.getStem())
                .type(request.getType())
                .subject(subject)
                .chapter(chapter)
                .bloomLevel(request.getBloomLevel())
                .score(request.getScore())
                .explanation(request.getExplanation())
                .referenceAnswer(request.getReferenceAnswer())
                .rubric(request.getRubric())
                .status(initialStatus)
                .createdBy(currentUser)
                .build();
        // ==========================
        // 4. Thêm Question Option
        // ==========================
        List<QuestionOption> options = new ArrayList<>();
        if (request.getOptions() != null) {
            for (QuestionOptionRequest optionRequest : request.getOptions()) {
                QuestionOption option = QuestionOption.builder()
                        .question(question)
                        .content(optionRequest.getContent())
                        .isCorrect(optionRequest.getIsCorrect())
                        .scoreWeight(optionRequest.getScore())
                        .build();
                options.add(option);
            }
        }
        question.setQuestionOptions(options);

        // UC16: normalize tags ^[a-z0-9-]+$
        Set<Tag> tags = new HashSet<>();
        if (request.getTags() != null) {
            for (String tagName : request.getTags()) {
                String normalized = tagName.trim().toLowerCase().replaceAll("[^a-z0-9-]", "");
                if (normalized.isBlank()) continue;
                Tag existingTag = tagRepository.findByName(normalized);
                if (existingTag == null) {
                    existingTag = tagRepository.save(Tag.builder().name(normalized).build());
                }
                tags.add(existingTag);
            }
        }
        question.setTags(tags);

        // ==========================
        // 5. Save
        // ==========================
        Question saved = questionRepository.save(question);

        // UC16: Audit log
        auditLogService.log(currentUser.getId(), username, AuditAction.QUESTION_CREATE,
                "{\"questionId\":\"" + saved.getId() + "\"}");

        // ==========================
        // 6. Response
        // ==========================
        return QuestionMapper.toResponse(saved);
    }


    // 4. Cập nhật câu hỏi — UC17
    @Transactional
    public QuestionResponse update(Long id, UpdateQuestionRequest request) {
        Question question = questionRepository.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy câu hỏi"));

        // UC17 E1: Chỉ owner hoặc ADMIN mới được sửa
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng."));
        boolean isOwner = question.getCreatedBy() != null
                && question.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getCode().equals("ADMIN"));
        if (!isOwner && !isAdmin) {
            throw new BadRequestException("UC17 E1: Bạn không có quyền chỉnh sửa câu hỏi này.");
        }

        // UC17: Không sửa câu hỏi đang APPROVED và đã có trong bài thi sắp diễn ra
        if (question.getStatus() == QuestionStatus.APPROVED
                && question.getUsageCount() != null && question.getUsageCount() > 0) {
            throw new BadRequestException(
                    "UC17 E2: Câu hỏi đang được dùng trong bài thi, không thể chỉnh sửa trực tiếp. " +
                    "Vui lòng tạo bản sao hoặc liên hệ quản trị viên.");
        }

        String plainText = Jsoup.parse(request.getStem()).text().trim();

        if (plainText.length() < 10 ) {
            throw new BadRequestException("Nội dung câu hỏi quá ngắn.");
        }
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new BadRequestException("Môn học không tồn tại."));
        // Validate Chapter
        Chapter chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new BadRequestException("Chương không tồn tại."));
        // Chapter phải thuộc Subject
        if (!chapter.getSubject().getId().equals(subject.getId())) {
            throw new BadRequestException("Chương không thuộc môn học đã chọn.");
        }
        // câu hỏi 1 đáp án đúng
        if (request.getType() == QuestionType.MCQ_SINGLE) {
            if (request.getOptions() == null || request.getOptions().isEmpty()) {
                throw new BadRequestException("Phương án không hợp lệ ");
            }
            if (request.getOptions().size() < 2 || request.getOptions().size() > 8) {
                throw new BadRequestException("Số phương án từ 2 đến 8.");
            }

            long correctCount = request.getOptions()
                    .stream()
                    .filter(QuestionOptionRequest::getIsCorrect)
                    .count();

            if (correctCount == 0) {
                throw new BadRequestException("Phải chọn 1 đáp án đúng.");
            }

            if (correctCount > 1) {
                throw new BadRequestException("Loại MCQ-Single chỉ cho phép 1 đáp án đúng.");
            }
        }

        // câu hỏi nhiều đáp án đúng
        if (request.getType() == QuestionType.MCQ_MULTIPLE) {
            long correctCount = request.getOptions()
                    .stream()
                    .filter(QuestionOptionRequest::getIsCorrect)
                    .count();

            if (correctCount == 0) {
                throw new BadRequestException("MCQ-Multiple phải có ít nhất 1 đáp án đúng.");
            }
        }
        // tự luận ngắn
        if (request.getType() == QuestionType.ESSAY) {

            if (request.getOptions() != null
                    && !request.getOptions().isEmpty()) {
                throw new BadRequestException(
                        "Câu hỏi tự luận không có phương án trả lời"
                );
            }
            if (request.getReferenceAnswer() == null) {
                throw new BadRequestException(
                        "Tự luận cần có đáp án tham khảo"
                );
            }
        }
        // câu hỏi đúng sai
        if (request.getType() == QuestionType.TRUE_FALSE) {
            if (request.getOptions() == null
                    || request.getOptions().size() != 2) {
                throw new BadRequestException(
                        "Câu hỏi Đúng/Sai phải có 2 phương án."
                );
            }
            long correctCount = request.getOptions()
                    .stream()
                    .filter(QuestionOptionRequest::getIsCorrect)
                    .count();
            if (correctCount != 1) {
                throw new BadRequestException(
                        "Câu hỏi Đúng/Sai phải có đúng 1 đáp án đúng."
                );
            }
            List<String> values = request.getOptions()
                    .stream()
                    .map(o -> o.getContent().trim().toLowerCase())
                    .toList();
            boolean valid =
                    values.contains("đúng")
                            && values.contains("sai");
            if (!valid) {
                throw new BadRequestException(
                        "Phương án của câu hỏi Đúng/Sai phải là Đúng và Sai."
                );
            }
        }
        question.setStem(request.getStem());
        question.setType(request.getType());
        question.setBloomLevel(request.getBloomLevel());
        question.setScore(request.getScore());
        question.setExplanation(request.getExplanation());
        question.setReferenceAnswer(request.getReferenceAnswer());
        question.setRubric(request.getRubric());

        question.getQuestionOptions().clear();
        List<QuestionOption> questionOptions = new ArrayList<>();
        for (QuestionOptionRequest optionRequest : request.getOptions()) {
            QuestionOption questionOption = QuestionOption.builder()
                    .content(optionRequest.getContent())
                    .isCorrect(optionRequest.getIsCorrect())
                    .scoreWeight(optionRequest.getScore())
                    .question(question)
                    .build();
            questionOptions.add(questionOption);
        }
        question.setQuestionOptions(questionOptions);
        Question saved = questionRepository.save(question);

        question.getTags().clear();
        Set<Tag> tags = new HashSet<>();
        if (request.getTags() != null) {
            for (String tag : request.getTags()) {
                Tag existingTag = tagRepository.findByName(tag);
                if (existingTag == null) {
                    existingTag = Tag.builder()
                            .name(tag)
                            .build();
                    existingTag = tagRepository.save(existingTag);
                }
                tags.add(existingTag);
            }
        }
        question.setTags(tags);

        // Audit log UC17
        auditLogService.log(currentUser.getId(), currentUsername, AuditAction.QUESTION_UPDATE,
                "{\"questionId\":" + id + "}");

        return QuestionMapper.toResponse(saved);
    }

    // 5. Xóa câu hỏi — UC18: soft delete, nếu usage_count>0 thì chuyển ARCHIVED
    @Transactional
    public void delete(Long id) {
        Question question = questionRepository.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy câu hỏi"));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng."));

        boolean isOwner = question.getCreatedBy() != null
                && question.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getCode().equals("ADMIN"));
        if (!isOwner && !isAdmin) {
            throw new BadRequestException("Bạn không có quyền xóa câu hỏi này.");
        }

        // BR-033: Nếu câu hỏi đang được dùng → chuyển ARCHIVED thay vì xóa vật lý
        if (question.getUsageCount() != null && question.getUsageCount() > 0) {
            question.setStatus(QuestionStatus.ARCHIVED);
            question.setDeletedAt(LocalDateTime.now());
            questionRepository.save(question);
            auditLogService.log(currentUser.getId(), username, AuditAction.QUESTION_ARCHIVE,
                    "{\"questionId\":" + id + ",\"reason\":\"has_usage\"}");
            return;
        }

        // Soft delete: không dùng → xóa mềm
        questionRepository.softDelete(id, LocalDateTime.now());
        auditLogService.log(currentUser.getId(), username, AuditAction.QUESTION_DELETE,
                "{\"questionId\":" + id + "}");
    }

    // UC18 Archive riêng biệt (endpoint PATCH /{id}/archive)
    @Transactional
    public void archive(Long id) {
        Question question = questionRepository.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy câu hỏi"));

        if (question.getStatus() == QuestionStatus.ARCHIVED) {
            throw new BadRequestException("Câu hỏi đã ở trạng thái ARCHIVED.");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng."));

        question.setStatus(QuestionStatus.ARCHIVED);
        question.setDeletedAt(LocalDateTime.now());
        questionRepository.save(question);

        auditLogService.log(currentUser.getId(), username, AuditAction.QUESTION_ARCHIVE,
                "{\"questionId\":" + id + "}");
    }


    // 6. Bulk cập nhật Subject và Chapter cho nhiều Question
    @Transactional
    public int updateQuestionClassification(QuestionClassificationRequest request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new com.hrm.project_spring.exception.ResourceNotFoundException("Môn học không tồn tại với ID: " + request.getSubjectId()));

        Chapter chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new com.hrm.project_spring.exception.ResourceNotFoundException("Chương không tồn tại với ID: " + request.getChapterId()));

        if (!chapter.getSubject().getId().equals(subject.getId())) {
            throw new BadRequestException("Chương không thuộc môn học đã chọn.");
        }

        if (request.getQuestionIds() == null || request.getQuestionIds().isEmpty()) {
            return 0;
        }

        List<Question> questions = questionRepository.findAllById(request.getQuestionIds());

        if (questions.size() != request.getQuestionIds().size()) {
            throw new com.hrm.project_spring.exception.ResourceNotFoundException("Một hoặc nhiều câu hỏi không tồn tại.");
        }

        for (Question question : questions) {
            question.setSubject(subject);
            question.setChapter(chapter);
            question.setUpdatedAt(LocalDateTime.now());
        }

        questionRepository.saveAll(questions);
        return questions.size();
    }
}
