package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.subject.SubjectRequest;
import com.hrm.project_spring.dto.subject.SubjectResponse;
import com.hrm.project_spring.entity.Subject;
import com.hrm.project_spring.mapper.SubjectMapper;
import com.hrm.project_spring.repository.QuestionRepository;
import com.hrm.project_spring.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        if (subjectRepository.existsByCode(request.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã môn học đã tồn tại: " + request.getCode());
        }

        Subject subject = Subject.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .build();

        return SubjectMapper.toResponse(subjectRepository.save(subject));
    }

    @Transactional(readOnly = true)
    public PageResponse<SubjectResponse> getAllSubjects(int pageNo, int pageSize) {
        Page<Subject> page = subjectRepository.findAll(PageRequest.of(pageNo, pageSize));
        List<SubjectResponse> content = page.getContent().stream()
                .map(SubjectMapper::toResponse)
                .toList();

        return PageResponse.<SubjectResponse>builder()
                .content(content)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> getAllSubjectsList() {
        return subjectRepository.findAll().stream()
                .map(SubjectMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học có ID: " + id));
        return SubjectMapper.toResponse(subject);
    }

    @Transactional
    public SubjectResponse updateSubject(Long id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học có ID: " + id));

        boolean hasQuestions = questionRepository.existsBySubjectId(id);

        if (!subject.getCode().equals(request.getCode())) {
            if (hasQuestions) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể sửa mã môn học khi đã có câu hỏi trong môn học này");
            }
            if (subjectRepository.existsByCodeAndIdNot(request.getCode(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã môn học đã tồn tại: " + request.getCode());
            }
            subject.setCode(request.getCode());
        }

        subject.setName(request.getName());
        subject.setDescription(request.getDescription());

        return SubjectMapper.toResponse(subjectRepository.save(subject));
    }

    @Transactional
    public void deleteSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học có ID: " + id));

        if (questionRepository.existsBySubjectId(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa môn học khi vẫn còn câu hỏi liên quan");
        }

        subjectRepository.delete(subject);
    }
}
