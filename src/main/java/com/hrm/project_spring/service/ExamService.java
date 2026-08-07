package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.exam.ExamDetailResponse;
import com.hrm.project_spring.dto.exam.ExamListResponse;
import com.hrm.project_spring.dto.exam.ExamRequest;
import com.hrm.project_spring.dto.student.StudentResponse;
import com.hrm.project_spring.entity.ClassRoom;
import com.hrm.project_spring.entity.Exam;
import com.hrm.project_spring.entity.Subject;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.AuditAction;
import com.hrm.project_spring.enums.ExamStatus;
import com.hrm.project_spring.exception.BadRequestException;
import com.hrm.project_spring.mapper.ExamMapper;
import com.hrm.project_spring.repository.ClassRoomRepository;
import com.hrm.project_spring.repository.ExamRepository;
import com.hrm.project_spring.repository.SubjectRepository;
import com.hrm.project_spring.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final ClassRoomRepository classRoomRepository;
    private final SubjectRepository subjectRepository;
    private final AuditLogService auditLogService;

    // ─── UC25: Lấy danh sách kỳ thi ───────────────────────────────────────────

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public ExamDetailResponse getExamById(Long id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kỳ thi không tìm thấy."));
        return ExamMapper.toDetailResponse(exam);
    }

    // ─── UC25: Tạo kỳ thi ─────────────────────────────────────────────────────

    @Transactional
    public ExamDetailResponse create(ExamRequest request, HttpServletRequest httpRequest) {
        validateCreate(request);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không hợp lệ."));

        // UC25: Validate và lấy Subject từ subjectId
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new BadRequestException("Môn học không hợp lệ."));

        // UC25 A1: nếu publish = true → PUBLISHED, ngược lại = DRAFT
        ExamStatus initialStatus = request.isPublish() ? ExamStatus.PUBLISHED : ExamStatus.DRAFT;

        Exam exam = Exam.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .semester(request.getSemester())
                .academicYear(request.getAcademicYear())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .subject(subject)
                .status(initialStatus)
                .createdBy(user)
                .owner(user)
                .build();

        Exam saved = examRepository.save(exam);

        // UC25: Audit log exam:create
        auditLogService.log(user.getId(), username, AuditAction.EXAM_CREATE, httpRequest,
                "{\"examId\":" + saved.getId() + ",\"code\":\"" + saved.getCode() + "\"}");

        return ExamMapper.toDetailResponse(saved);
    }

    // ─── UC26: Cập nhật kỳ thi ────────────────────────────────────────────────

    @Transactional
    public ExamDetailResponse update(Long id, ExamRequest request, HttpServletRequest httpRequest) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kỳ thi không tìm thấy."));

        // UC26 E2: Không cho đổi code khi đã PUBLISHED
        if (exam.getStatus() == ExamStatus.PUBLISHED
                && request.getCode() != null
                && !exam.getCode().equals(request.getCode())) {
            throw new BadRequestException("Kỳ thi đã Published, không thể đổi mã kỳ thi.");
        }

        // UC26 A1: Cảnh báo khi sửa exam đang PUBLISHED — cần confirm = true
        if (exam.getStatus() == ExamStatus.PUBLISHED && !request.isConfirm()) {
            throw new BadRequestException(
                    "Kỳ thi đang Published, thay đổi có thể ảnh hưởng Student. Gửi lại với confirm=true để tiếp tục.");
        }

        // UC26: Validate chỉ các field được gửi (partial update)
        validateUpdate(request, id);

        // PATCH semantics: chỉ update field không null
        if (request.getName() != null && !request.getName().isBlank()) {
            exam.setName(request.getName());
        }
        if (request.getDescription() != null) {
            exam.setDescription(request.getDescription());
        }
        if (request.getSemester() != null && !request.getSemester().isBlank()) {
            exam.setSemester(request.getSemester());
        }
        if (request.getAcademicYear() != null && !request.getAcademicYear().isBlank()) {
            exam.setAcademicYear(request.getAcademicYear());
        }
        if (request.getStartDate() != null) {
            exam.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            exam.setEndDate(request.getEndDate());
        }
        // UC26 E2: code chỉ được cập nhật khi không null và exam chưa PUBLISHED
        if (request.getCode() != null && exam.getStatus() != ExamStatus.PUBLISHED) {
            exam.setCode(request.getCode());
        }
        exam.setUpdatedAt(LocalDateTime.now());

        // Cập nhật subject nếu subjectId thay đổi
        if (request.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new BadRequestException("Môn học không hợp lệ."));
            exam.setSubject(subject);
        }

        // Không cho phép client tự đặt status qua PATCH /exams/{id}
        // Status được quản lý riêng qua /publish và /archive

        Exam saved = examRepository.save(exam);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log(null, username, AuditAction.EXAM_UPDATE, httpRequest,
                "{\"examId\":" + id + "}");

        return ExamMapper.toDetailResponse(saved);
    }

    // ─── UC26: Xóa kỳ thi (soft delete) ──────────────────────────────────────

    @Transactional
    public ExamDetailResponse deleteExam(Long id, HttpServletRequest httpRequest) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kỳ thi không tìm thấy."));

        // UC26 E1: Không xóa nếu đã có attempt completed
        if (examRepository.hasCompletedAttempts(id)) {
            throw new BadRequestException(
                    "Kỳ thi đã có lượt thi, không thể xóa. Có thể Archive để ẩn khỏi danh sách.");
        }

        // Soft delete
        exam.setDeletedAt(LocalDateTime.now());
        Exam saved = examRepository.save(exam);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log(null, username, AuditAction.EXAM_DELETE, httpRequest,
                "{\"examId\":" + id + "}");

        // Trả về response với deletedAt để FE không cần gọi thêm GET
        return ExamMapper.toDetailResponse(saved);
    }

    // ─── UC25 A1: Publish kỳ thi ──────────────────────────────────────────────

    @Transactional
    public ExamDetailResponse publish(Long id, HttpServletRequest httpRequest) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kỳ thi không tìm thấy."));

        if (exam.getStatus() == ExamStatus.ARCHIVED) {
            throw new BadRequestException("Kỳ thi đã Archived, không thể Publish.");
        }

        exam.setStatus(ExamStatus.PUBLISHED);
        exam.setUpdatedAt(LocalDateTime.now());
        Exam saved = examRepository.save(exam);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log(null, username, AuditAction.EXAM_UPDATE, httpRequest,
                "{\"examId\":" + id + ",\"action\":\"publish\"}");

        return ExamMapper.toDetailResponse(saved);
    }

    // ─── UC26 / BR-043: Archive kỳ thi ───────────────────────────────────────

    @Transactional
    public ExamDetailResponse archive(Long id, HttpServletRequest httpRequest) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kỳ thi không tìm thấy."));

        if (exam.getStatus() == ExamStatus.ARCHIVED) {
            throw new BadRequestException("Kỳ thi đã ở trạng thái ARCHIVED.");
        }

        exam.setStatus(ExamStatus.ARCHIVED);
        exam.setUpdatedAt(LocalDateTime.now());
        Exam saved = examRepository.save(exam);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log(null, username, AuditAction.EXAM_UPDATE, httpRequest,
                "{\"examId\":" + id + ",\"action\":\"archive\"}");

        return ExamMapper.toDetailResponse(saved);
    }

    // ─── Student management ───────────────────────────────────────────────────

    public ExamDetailResponse assignStudentsToExam(Long examId, Set<Long> studentIds) {
        Exam exam = examRepository.findByIdWithStudents(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kỳ thi không tìm thấy."));
        Set<User> students = getValidStudents(studentIds);
        students.removeAll(exam.getStudents());
        exam.getStudents().addAll(students);
        return ExamMapper.toDetailResponse(examRepository.save(exam));
    }

    public ExamDetailResponse removeStudentsFromExam(Long examId, Set<Long> studentIds) {
        Exam exam = examRepository.findByIdWithStudents(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kỳ thi không tìm thấy."));
        exam.getStudents().removeIf(u -> studentIds.contains(u.getId()));
        return ExamMapper.toDetailResponse(examRepository.save(exam));
    }

    public ExamDetailResponse assignClassToExam(Long examId, Long classId) {
        Exam exam = examRepository.findByIdWithStudents(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kỳ thi không tìm thấy."));
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tìm thấy."));
        exam.getStudents().addAll(classRoom.getStudents());
        return ExamMapper.toDetailResponse(examRepository.save(exam));
    }

    public Set<StudentResponse> getStudentsByExamId(Long examId) {
        Exam exam = examRepository.findByIdWithStudents(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kỳ thi không tìm thấy."));
        return exam.getStudents().stream()
                .map(u -> StudentResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .build())
                .collect(Collectors.toSet());
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Set<User> getValidStudents(Set<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return Set.of();
        return new HashSet<>(userRepository.findAllById(studentIds));
    }

    /**
     * UC25: Validate khi TẠO mới kỳ thi.
     */
    private void validateCreate(ExamRequest request) {
        // Kiểm tra tên
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Tên kỳ thi không được để trống.");
        }
        // Kiểm tra code trùng
        if (examRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Mã kỳ thi 3-30 ký tự và không trùng.");
        }
        validateDates(request);
    }

    /**
     * UC26: Validate khi CẬP NHẬT kỳ thi (PATCH — chỉ validate field được gửi).
     * - name: nếu gửi thì phải 5-150 ký tự.
     * - code: nếu gửi thì phải unique (loại trừ chính exam đang sửa).
     * - dates: nếu gửi thì phải hợp lệ.
     */
    private void validateUpdate(ExamRequest request, Long examId) {
        // Chỉ validate name nếu được gửi
        if (request.getName() != null && request.getName().isBlank()) {
            throw new BadRequestException("Tên kỳ thi không được để trống.");
        }
        // Chỉ check trùng code nếu code được gửi
        if (request.getCode() != null
                && examRepository.existsByCodeAndIdNot(request.getCode(), examId)) {
            throw new BadRequestException("Mã kỳ thi 3-30 ký tự và không trùng.");
        }
        // Validate ngày chỉ khi ít nhất 1 trong 2 được gửi
        if (request.getStartDate() != null || request.getEndDate() != null) {
            validateDates(request);
        }
    }

    /**
     * UC25 / UC26: Validate ngày bắt đầu và kết thúc.
     * - startDate không được trước now - 7 ngày.
     * - endDate phải sau startDate và không quá startDate + 365 ngày.
     */
    private void validateDates(ExamRequest request) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        if (startDate == null || endDate == null) return;

        LocalDate minStart = LocalDate.now().minusDays(7);
        if (startDate.isBefore(minStart)) {
            throw new BadRequestException("Ngày bắt đầu không hợp lệ (không được sớm hơn 7 ngày trước hiện tại).");
        }
        if (!endDate.isAfter(startDate)) {
            throw new BadRequestException("Ngày kết thúc không hợp lệ (phải sau ngày bắt đầu).");
        }
        if (endDate.isAfter(startDate.plusDays(365))) {
            throw new BadRequestException("Ngày kết thúc không hợp lệ (không quá 365 ngày sau ngày bắt đầu).");
        }
    }
}

