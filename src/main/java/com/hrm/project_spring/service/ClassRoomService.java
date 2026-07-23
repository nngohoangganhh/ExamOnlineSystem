package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.classroom.AssignStudentsToClassRequest;
import com.hrm.project_spring.dto.classroom.ClassRoomRequest;
import com.hrm.project_spring.dto.classroom.ClassRoomResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.student.StudentResponse;
import com.hrm.project_spring.entity.ClassRoom;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.repository.ClassRoomRepository;
import com.hrm.project_spring.repository.ExamRepository;
import com.hrm.project_spring.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassRoomService {

    private final ClassRoomRepository classRoomRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;

    @Transactional
    public ClassRoomResponse createClassRoom(ClassRoomRequest request) {
        // UC15-E1: 409 CONFLICT khi mã lớp đã tồn tại
        if (classRoomRepository.existsByCode(request.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã lớp đã tồn tại");
        }

        // UC15: Validate teacherId nếu có
        Long teacherId = null;
        if (request.getTeacherId() != null) {
            User teacher = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giáo viên không tồn tại"));
            boolean isTeacher = teacher.getRoles().stream()
                    .anyMatch(r -> "TEACHER".equalsIgnoreCase(r.getCode()));
            if (!isTeacher) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User được chỉ định phải có role TEACHER");
            }
            teacherId = teacher.getId();
        }

        ClassRoom classRoom = ClassRoom.builder()
                .code(request.getCode())
                .name(request.getName())
                .academicYear(request.getAcademicYear())
                .description(request.getDescription())
                .teacherId(teacherId)
                .build();

        classRoom = classRoomRepository.save(classRoom);
        return mapToResponse(classRoom);
    }

    @Transactional
    public ClassRoomResponse updateClassRoom(Long id, ClassRoomRequest request) {
        ClassRoom classRoom = classRoomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại"));

        // UC15-A1: Không cho đổi mã lớp nếu đã có exam gán
        boolean codeChanged = !classRoom.getCode().equals(request.getCode());
        if (codeChanged) {
            // Kiểm tra đã có kỳ thi gán vào lớp này chưa (qua exam_students)
            boolean hasExam = examRepository.existsByStudentId(classRoom.getId());
            if (hasExam) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Không thể đổi mã lớp vì lớp này đã được gán vào kỳ thi");
            }
            // Kiểm tra mã lớp mới không bị trùng
            if (classRoomRepository.existsByCode(request.getCode())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã lớp đã tồn tại");
            }
        }

        // Validate teacherId nếu có
        Long teacherId = classRoom.getTeacherId();
        if (request.getTeacherId() != null) {
            User teacher = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giáo viên không tồn tại"));
            boolean isTeacher = teacher.getRoles().stream()
                    .anyMatch(r -> "TEACHER".equalsIgnoreCase(r.getCode()));
            if (!isTeacher) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User được chỉ định phải có role TEACHER");
            }
            teacherId = teacher.getId();
        }

        classRoom.setCode(request.getCode());
        classRoom.setName(request.getName());
        classRoom.setAcademicYear(request.getAcademicYear());
        classRoom.setDescription(request.getDescription());
        classRoom.setTeacherId(teacherId);

        classRoom = classRoomRepository.save(classRoom);
        return mapToResponse(classRoom);
    }

    /**
     * UC15-E2: Chỉ xóa lớp khi không còn sinh viên nào.
     */
    @Transactional
    public void deleteClassRoom(Long id) {
        ClassRoom classRoom = classRoomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại"));

        // UC15-A2: Từ chối nếu lớp còn sinh viên
        if (classRoom.getStudents() != null && !classRoom.getStudents().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa lớp học còn " + classRoom.getStudents().size() + " sinh viên. Hãy gỡ sinh viên trước.");
        }

        classRoomRepository.delete(classRoom);
    }

    @Transactional
    public PageResponse<ClassRoomResponse> getAllClassRooms(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<ClassRoom> page = classRoomRepository.findAll(pageable);

        List<ClassRoomResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<ClassRoomResponse>builder()
                .content(content)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public ClassRoomResponse getClassRoom(Long id) {
        ClassRoom classRoom = classRoomRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học"));
        return mapToResponse(classRoom);
    }



    @Transactional
    public ClassRoomResponse    assignStudents(Long id, AssignStudentsToClassRequest request) {
        ClassRoom classRoom = classRoomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại"));

        List<User> students = userRepository.findAllById(request.getStudentIds());

        // UC15: Check duplicate trước khi add
        Set<Long> existingStudentIds = classRoom.getStudents().stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        List<User> newStudents = students.stream()
                .filter(s -> !existingStudentIds.contains(s.getId()))
                .toList();

        classRoom.getStudents().addAll(newStudents);
        classRoom = classRoomRepository.save(classRoom);
        return mapToResponse(classRoom);
    }

    @Transactional
    public ClassRoomResponse removeStudents(Long id, AssignStudentsToClassRequest request) {
        ClassRoom classRoom = classRoomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại"));

        List<User> students = userRepository.findAllById(request.getStudentIds());
        classRoom.getStudents().removeAll(students);

        classRoom = classRoomRepository.save(classRoom);
        return mapToResponse(classRoom);
    }

    private ClassRoomResponse mapToResponse(ClassRoom classRoom) {
        User teacher = null;
    if (classRoom.getTeacherId() != null) {
        teacher = userRepository.findById(classRoom.getTeacherId()).orElse(null);
    }
        return ClassRoomResponse.builder()
                .id(classRoom.getId())
                .code(classRoom.getCode())
                .name(classRoom.getName())
                .description(classRoom.getDescription())
                .academicYear(classRoom.getAcademicYear())
                .createdAt(classRoom.getCreatedAt())
                .studentCount(classRoom.getStudents() != null ? classRoom.getStudents().size() : 0)
                .teacherName(teacher!= null ? teacher.getFullName(): null)
                .build();
    }
}
