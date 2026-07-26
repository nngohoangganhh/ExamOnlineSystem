package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.chapter.ChapterRequest;
import com.hrm.project_spring.dto.chapter.ChapterResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.entity.Chapter;
import com.hrm.project_spring.entity.Subject;
import com.hrm.project_spring.mapper.ChapterMapper;
import com.hrm.project_spring.repository.ChapterRepository;
import com.hrm.project_spring.repository.QuestionRepository;
import com.hrm.project_spring.repository.SubjectRepository;
import com.hrm.project_spring.exception.ResourceNotFoundException;
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
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public ChapterResponse createChapter(ChapterRequest request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học có ID: " + request.getSubjectId()));

        if (chapterRepository.existsBySubjectIdAndCode(request.getSubjectId(), request.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã chương đã tồn tại trong môn học này: " + request.getCode());
        }

        Chapter chapter = Chapter.builder()
                .subject(subject)
                .name(request.getName())
                .code(request.getCode())
                .orderNum(request.getOrder())
                .description(request.getDescription())
                .build();

        return ChapterMapper.toResponse(chapterRepository.save(chapter));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChapterResponse> getAllChapters(int pageNo, int pageSize) {
        Page<Chapter> page = chapterRepository.findAll(PageRequest.of(pageNo, pageSize));
        List<ChapterResponse> content = page.getContent().stream()
                .map(ChapterMapper::toResponse)
                .toList();

        return PageResponse.<ChapterResponse>builder()
                .content(content)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ChapterResponse getChapterById(Long id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương có ID: " + id));
        return ChapterMapper.toResponse(chapter);
    }

    @Transactional(readOnly = true)
    public List<ChapterResponse> getChaptersBySubjectId(Long subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Không tìm thấy môn học có ID: " + subjectId);
        }
        return chapterRepository.findBySubjectIdOrderByOrderNumAsc(subjectId).stream()
                .map(ChapterMapper::toResponse)
                .toList();
    }

    @Transactional
    public ChapterResponse updateChapter(Long id, ChapterRequest request) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chương có ID: " + id));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học có ID: " + request.getSubjectId()));

        if (chapterRepository.existsBySubjectIdAndCodeAndIdNot(request.getSubjectId(), request.getCode(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã chương đã tồn tại trong môn học này: " + request.getCode());
        }

        chapter.setSubject(subject);
        chapter.setName(request.getName());
        chapter.setCode(request.getCode());
        chapter.setOrderNum(request.getOrder());
        chapter.setDescription(request.getDescription());

        return ChapterMapper.toResponse(chapterRepository.save(chapter));
    }

    @Transactional
    public void deleteChapter(Long id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chương có ID: " + id));

        if (questionRepository.existsByChapterId(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa chương khi vẫn còn câu hỏi liên quan");
        }

        chapterRepository.delete(chapter);
    }
}
