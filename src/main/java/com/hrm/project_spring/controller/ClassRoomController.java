package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.classroom.AssignStudentsToClassRequest;
import com.hrm.project_spring.dto.classroom.ClassRoomRequest;
import com.hrm.project_spring.dto.classroom.ClassRoomResponse;
import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.service.ClassRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassRoomController {

    private final ClassRoomService classRoomService;

    @PreAuthorize("hasAuthority('CLASS:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ClassRoomResponse>>> getAllClassRooms(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<ClassRoomResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách lớp học thành công")
                .data(classRoomService.getAllClassRooms(pageNo, pageSize))
                .build());
    }


    @PreAuthorize("hasAuthority('CLASS:READ')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassRoomResponse>> getClassRoom(@PathVariable Long id ){
        return ResponseEntity.ok(ApiResponse.<ClassRoomResponse>builder()
                .success(true)
                .code(200)
                .message("Lấy chi tiết lớp học thành công")
                .data(classRoomService.getClassRoom(id))
                .build());
    }

    @PreAuthorize("hasAuthority('CLASS:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<ClassRoomResponse>> createClassRoom(
            @Valid @RequestBody ClassRoomRequest request) {
        return ResponseEntity.ok(ApiResponse.<ClassRoomResponse>builder()
                .success(true)
                .code(200)
                .message("Tạo lớp học thành công")
                .data(classRoomService.createClassRoom(request))
                .build());
    }

    @PreAuthorize("hasAuthority('CLASS:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassRoomResponse>> updateClassRoom(
            @PathVariable Long id,
            @Valid @RequestBody ClassRoomRequest request) {
        return ResponseEntity.ok(ApiResponse.<ClassRoomResponse>builder()
                .success(true)
                .code(200)
                .message("Cập nhật lớp học thành công")
                .data(classRoomService.updateClassRoom(id, request))
                .build());
    }

    @PreAuthorize("hasAuthority('CLASS:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClassRoom(@PathVariable Long id) {
        classRoomService.deleteClassRoom(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Xóa lớp học thành công")
                .build());
    }

    @PreAuthorize("hasAuthority('CLASS:UPDATE')")
    @PostMapping("/{id}/students")
    public ResponseEntity<ApiResponse<ClassRoomResponse>> assignStudents(
            @PathVariable Long id,
            @Valid @RequestBody AssignStudentsToClassRequest request) {
        return ResponseEntity.ok(ApiResponse.<ClassRoomResponse>builder()
                .success(true)
                .code(200)
                .message("Gán sinh viên vào lớp thành công")
                .data(classRoomService.assignStudents(id, request))
                .build());
    }

    @PreAuthorize("hasAuthority('CLASS:UPDATE')")
    @DeleteMapping("/{id}/students")
    public ResponseEntity<ApiResponse<ClassRoomResponse>> removeStudents(
            @PathVariable Long id,
            @Valid @RequestBody AssignStudentsToClassRequest request) {
        return ResponseEntity.ok(ApiResponse.<ClassRoomResponse>builder()
                .success(true)
                .code(200)
                .message("Xóa sinh viên khỏi lớp thành công")
                .data(classRoomService.removeStudents(id, request))
                .build());
    }
}
