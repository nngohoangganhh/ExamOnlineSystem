package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.exam.ExamDetailResponse;
import com.hrm.project_spring.dto.exam.ExamListResponse;
import com.hrm.project_spring.dto.exam.ExamRequest;
import com.hrm.project_spring.dto.student.StudentResponse;
import com.hrm.project_spring.entity.ClassRoom;
import com.hrm.project_spring.entity.Exam;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.mapper.ExamMapper;
import com.hrm.project_spring.repository.ClassRoomRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamService {

    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final ClassRoomRepository classRoomRepository;

    private static final String ROLE_STUDENT = "STUDENT";

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
    public ExamDetailResponse getExamById(Long id) {
        Exam exam = examRepository.findByIdWithStudents(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam not found"));

        return ExamMapper.toDetailResponse(exam);
    }
    public ExamDetailResponse create(ExamRequest request) {
        validate(request);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
        Exam exam = Exam.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startTime(request.getStartTime().atDate(LocalDate.now()))
                .endTime(request.getEndTime().atDate(LocalDate.now()))
                .status(request.getStatus())
                .createdBy(user)
                .createdAt(LocalTime.now())
                .build();
        return ExamMapper.toDetailResponse(examRepository.save(exam));
    }
    public ExamDetailResponse update(Long id, ExamRequest request) {
        validate(request);
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam not found"));
        exam.setName(request.getName());
        exam.setDescription(request.getDescription());
        exam.setStartTime(request.getStartTime().atDate(LocalDate.from(LocalDateTime.now())));
        exam.setEndTime(request.getEndTime().atDate(LocalDate.from(LocalDateTime.now())));
        exam.setStatus(request.getStatus());
        return ExamMapper.toDetailResponse(examRepository.save(exam));
    }
    public void deleteExam(Long id) {
        if (!examRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found");
        }
        examRepository.deleteById(id);
    }


    public ExamDetailResponse assignStudentsToExam(Long examId, Set<Long> studentIds) {
        Exam exam = examRepository.findByIdWithStudents(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam not found"));

        Set<User> students = getValidStudents(studentIds);
        students.removeAll(exam.getStudents()); // tránh duplicate
        exam.getStudents().addAll(students);
        return ExamMapper.toDetailResponse(exam);
    }

    public ExamDetailResponse assignClassToExam(Long examId, Long classId) {
        Exam exam = examRepository.findByIdWithStudents(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam not found"));
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class not found"));

        Set<User> students = classRoom.getStudents();
        students.removeAll(exam.getStudents());
        exam.getStudents().addAll(students);
        return ExamMapper.toDetailResponse(exam);
    }

    public ExamDetailResponse removeStudentsFromExam(Long examId, Set<Long> studentIds) {
        Exam exam = examRepository.findByIdWithStudents(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam not found"));
        exam.getStudents().removeIf(u -> studentIds.contains(u.getId()));
        return ExamMapper.toDetailResponse(exam);
    }

    public Set<StudentResponse> getStudentsByExamId(Long examId) {
        Exam exam = examRepository.findByIdWithStudents(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam not found"));
        return exam.getStudents().stream()
                .map(u -> StudentResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .build())
                .collect(Collectors.toSet());
    }

    private Set<User> getValidStudents(Set<Long> ids) {
        // Lấy tất cả user tồn tại, chỉ giữ những user có role STUDENT
        Set<User> users = userRepository.findAllById(ids).stream()
                        .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> ROLE_STUDENT.equals(r.getCode())))
                        .collect(Collectors.toSet());
        if (users.isEmpty() && !ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Không tìm thấy user hợp lệ có role STUDENT");
        }
        return users;
    }

    private void validate(ExamRequest request) {
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start phải trước End");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên không được trống");
        }
    }
}
