package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.user.UserRequest;
import com.hrm.project_spring.dto.user.UserResponse;
import com.hrm.project_spring.dto.user.UserResponseDto;
import com.hrm.project_spring.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasAuthority('USER:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponseDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<UserResponseDto>>builder()
                        .success(true)
                        .code(200)
                        .message("Lấy danh sách thành công")
                        .data(userService.getAllUsers(pageNo,pageSize))
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('USER:READ')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .code(200)
                        .message("lấy user theo id thành cong")
                        .data(userService.getUserById(id))
                        .build()
        );
    }

    @PreAuthorize("hasAuthority('USER:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .code(200)
                        .message("tạo user thành công")
                        .data(userService.createUser(request))
                        .build()
        );
    }
    @PreAuthorize("hasAuthority('USER:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>>updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .code(200)
                        .message("sửa thành công")
                        .data(userService.updateUser(id,request))
                         .build());
    }
    @PreAuthorize("hasAuthority('USER:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
