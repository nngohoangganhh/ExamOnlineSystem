package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.exam.ExamDetailResponse;
import com.hrm.project_spring.dto.exam.ExamListResponse;
import com.hrm.project_spring.dto.exam.ExamRequest;
import com.hrm.project_spring.entity.Exam;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.ExamStatus;
import com.hrm.project_spring.mapper.ExamMapper;
import com.hrm.project_spring.repository.ExamRepository;
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
@Transactional
public class ExamService {

    private final ExamRepository examRepository;
    private final UserRepository userRepository;

    @Transactional
    public PageResponse<ExamListResponse> getAllExam(int pageNo, int pageSize) {
        Page<Exam> page = examRepository.findAll(PageRequest.of(pageNo, pageSize));
        List<ExamListResponse> data = page.getContent()
                .stream()
                .map(ExamMapper::toListResponse)
                .toList();
        return PageResponse.<ExamListResponse>builder()
                .content(data)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public ExamDetailResponse getExamById(Long id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam not found"));
        return ExamMapper.toDetailResponse(exam);
    }

    @Transactional
    public ExamDetailResponse create(ExamRequest request) {
        validate(request);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
        
        Exam exam = Exam.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .semester(request.getSemester())
                .academicYear(request.getAcademicYear())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus() != null ? request.getStatus() : ExamStatus.DRAFT)
                .createdBy(user)
                .owner(user)
                .build();
        return ExamMapper.toDetailResponse(examRepository.save(exam));
    }

    @Transactional
    public ExamDetailResponse update(Long id, ExamRequest request) {
        validate(request);
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam not found"));
        exam.setCode(request.getCode());
        exam.setName(request.getName());
        exam.setDescription(request.getDescription());
        exam.setSemester(request.getSemester());
        exam.setAcademicYear(request.getAcademicYear());
        exam.setStartDate(request.getStartDate());
        exam.setEndDate(request.getEndDate());
        exam.setStatus(request.getStatus());
        exam.setUpdatedAt(LocalDateTime.now());
        return ExamMapper.toDetailResponse(examRepository.save(exam));
    }

    @Transactional
    public void deleteExam(Long id) {
        if (!examRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found");
        }
        examRepository.deleteById(id);
    }

    private void validate(ExamRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null && request.getStartDate().isAfter(request.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be before end date");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be empty");
        }
    }
}
