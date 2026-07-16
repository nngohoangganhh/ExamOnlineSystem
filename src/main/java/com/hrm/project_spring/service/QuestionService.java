package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.question.QuestionRequest;
import com.hrm.project_spring.dto.question.QuestionResponse;
import com.hrm.project_spring.entity.Question;
import com.hrm.project_spring.entity.User;
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
//1
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
//2
    @Transactional
    public QuestionResponse getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        return QuestionMapper.toResponse(question);
    }
    @Transactional
    public QuestionResponse create(QuestionRequest request) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        Question question = Question.builder()
                .content(request.getContent())
                .questionType(request.getQuestionType())
                .difficulty(request.getDifficulty())
                .createdAt(LocalDateTime.now())
                .createdBy(currentUser)
                .build();
        Question saved = questionRepository.save(question);
        return QuestionMapper.toResponse(saved);
    }
//3
    @Transactional
    public QuestionResponse update(Long id, QuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        question.setContent(request.getContent());
        question.setQuestionType(request.getQuestionType());
        question.setDifficulty(request.getDifficulty());
        Question updated = questionRepository.save(question);
        return QuestionMapper.toResponse(updated);
    }
//4
    @Transactional
    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        questionRepository.deleteById(id);
    }
}
