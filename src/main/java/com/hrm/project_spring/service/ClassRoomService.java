package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.classroom.AssignStudentsToClassRequest;
import com.hrm.project_spring.dto.classroom.ClassRoomRequest;
import com.hrm.project_spring.dto.classroom.ClassRoomResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.entity.ClassRoom;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.repository.ClassRoomRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassRoomService {

    private final ClassRoomRepository classRoomRepository;
    private final UserRepository userRepository;

    @Transactional
    public ClassRoomResponse createClassRoom(ClassRoomRequest request) {
        if (classRoomRepository.existsByCode(request.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã lớp đã tồn tại");
        }

        ClassRoom classRoom = ClassRoom.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .build();

        classRoom = classRoomRepository.save(classRoom);
        return mapToResponse(classRoom);
    }

    @Transactional
    public ClassRoomResponse updateClassRoom(Long id, ClassRoomRequest request) {
        ClassRoom classRoom = classRoomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại"));

        if (!classRoom.getCode().equals(request.getCode()) && classRoomRepository.existsByCode(request.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã lớp đã tồn tại");
        }

        classRoom.setCode(request.getCode());
        classRoom.setName(request.getName());
        classRoom.setDescription(request.getDescription());

        classRoom = classRoomRepository.save(classRoom);
        return mapToResponse(classRoom);
    }

    public void deleteClassRoom(Long id) {
        ClassRoom classRoom = classRoomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại"));
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
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, " Không tìm thấy lớp học "));
        return mapToResponse(classRoom);
    }


    @Transactional
    public ClassRoomResponse assignStudents(Long id, AssignStudentsToClassRequest request) {
        ClassRoom classRoom = classRoomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại"));

        List<User> students = userRepository.findAllById(request.getStudentIds());
        classRoom.getStudents().addAll(students);

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
        return ClassRoomResponse.builder()
                .id(classRoom.getId())
                .code(classRoom.getCode())
                .name(classRoom.getName())
                .description(classRoom.getDescription())
                .createdAt(classRoom.getCreatedAt())
                .studentCount(classRoom.getStudents() != null ? classRoom.getStudents().size() : 0)
                .build();
    }
}
