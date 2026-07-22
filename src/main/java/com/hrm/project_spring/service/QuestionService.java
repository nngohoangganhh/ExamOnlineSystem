package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.question.QuestionRequest;
import com.hrm.project_spring.dto.question.QuestionResponse;
import com.hrm.project_spring.entity.Question;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.QuestionStatus;
import com.hrm.project_spring.enums.QuestionType;
import com.hrm.project_spring.mapper.QuestionMapper;
import com.hrm.project_spring.repository.QuestionRepository;
import com.hrm.project_spring.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    // 1. Lấy tất cả câu hỏi (phân trang)
    @Transactional
    public PageResponse<QuestionResponse> getAllQuestion(int pageNo, int pageSize) {
        Page<Question> page = questionRepository.findAll(PageRequest.of(pageNo, pageSize));
        List<QuestionResponse> data = page.getContent()
                .stream()
                .map(QuestionMapper::toResponse)
                .toList();
        return PageResponse.<QuestionResponse>builder()
                .content(data)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // 2. Lấy câu hỏi theo id
    @Transactional
    public QuestionResponse getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy câu hỏi"));
        return QuestionMapper.toResponse(question);
    }

    // 3. Tạo câu hỏi mới
    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long currentUserId = userRepository.findByUsername(username)
                .map(u -> u.getId())
                .orElse(null);

        // Mapping: QuestionRequest.questionType (String) → QuestionType enum
        QuestionType type = null;
        if (request.getQuestionType() != null) {
            try {
                type = QuestionType.valueOf(request.getQuestionType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Loại câu hỏi không hợp lệ: " + request.getQuestionType());
            }
        }

        // Mapping: QuestionRequest.difficulty (String) → bloomLevel (Integer)
        Integer bloomLevel = null;
        if (request.getDifficulty() != null && !request.getDifficulty().isBlank()) {
            try {
                bloomLevel = Integer.parseInt(request.getDifficulty());
            } catch (NumberFormatException e) {
                // Nếu difficulty là text (easy/medium/hard), map sang bloomLevel 1/2/3
                bloomLevel = switch (request.getDifficulty().toLowerCase()) {
                    case "easy" -> 1;
                    case "medium" -> 2;
                    case "hard" -> 3;
                    default -> 1;
                };
            }
        }

        Question question = Question.builder()
                .stem(request.getContent())
                .type(type)
                .bloomLevel(bloomLevel != null ? bloomLevel : 1)
                .status(QuestionStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .createdBy(currentUserId)
                .build();

        Question saved = questionRepository.save(question);
        return QuestionMapper.toResponse(saved);
    }

    // 4. Cập nhật câu hỏi
    @Transactional
    public QuestionResponse update(Long id, QuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy câu hỏi"));

        if (request.getContent() != null) {
            question.setStem(request.getContent());
        }

        if (request.getQuestionType() != null) {
            try {
                question.setType(QuestionType.valueOf(request.getQuestionType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Loại câu hỏi không hợp lệ: " + request.getQuestionType());
            }
        }

        if (request.getDifficulty() != null && !request.getDifficulty().isBlank()) {
            try {
                question.setBloomLevel(Integer.parseInt(request.getDifficulty()));
            } catch (NumberFormatException e) {
                question.setBloomLevel(switch (request.getDifficulty().toLowerCase()) {
                    case "easy" -> 1;
                    case "medium" -> 2;
                    case "hard" -> 3;
                    default -> 1;
                });
            }
        }

        question.setUpdatedAt(LocalDateTime.now());
        Question updated = questionRepository.save(question);
        return QuestionMapper.toResponse(updated);
    }

    // 5. Xóa câu hỏi
    @Transactional
    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy câu hỏi");
        }
        questionRepository.deleteById(id);
    }
}
