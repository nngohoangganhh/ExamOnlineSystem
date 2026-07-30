package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.user.request.*;
import com.hrm.project_spring.dto.user.response.CreateUserResponse;
import com.hrm.project_spring.dto.user.response.ImportUserResponse;
import com.hrm.project_spring.dto.user.response.UserResponse;
import com.hrm.project_spring.dto.user.response.UserResponseDto;
import com.hrm.project_spring.enums.UserStatus;
import com.hrm.project_spring.service.user.UserImportService;
import com.hrm.project_spring.service.user.UserExportService;
import com.hrm.project_spring.service.user.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserExportService userExportService;
    private final UserImportService userImportService;

    // ======================== USER CRUD (Admin) ========================

    @PreAuthorize("hasAuthority('USER:READ')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponseDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<UserResponseDto>>builder()
                        .success(true)
                        .code(200)
                        .message("Lấy danh sách thành công")
                        .data(userService.getAllUsers(pageNo, pageSize))
                        .build()
        );
    }

    /**
     * UC14: Tìm kiếm và lọc danh sách user.
     * Hỗ trợ: keyword, roleId, classId, status, createdFrom, createdTo, includeDeleted.
     */
    @PreAuthorize("hasAuthority('USER:READ')")
    @GetMapping("/search")

    public ResponseEntity<ApiResponse<PageResponse<UserResponseDto>>> searchUsers(

            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) UserStatus status,
            // Trước đây nhận String rồi tự gọi LocalDate.parse(...) thủ công:
            // nếu client nhập giá trị không phải ngày hợp lệ (vd "123"),
            // DateTimeParseException bị ném ra KHÔNG có handler riêng trong
            // GlobalExceptionHandler -> rơi xuống handler Exception.class chung
            // -> trả về 500 thay vì 400.
            // Để Spring tự bind trực tiếp sang LocalDate: nếu parse lỗi, Spring
            // ném MethodArgumentTypeMismatchException, đã có handler xử lý sẵn
            // trong GlobalExceptionHandler -> trả về đúng 400 kèm message rõ ràng.
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate createdTo,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {

        UserSearchRequest search = new UserSearchRequest();
        search.setKeyword(keyword);
        search.setRoleId(roleId);
        search.setClassId(classId);
        search.setStatus(status);
        search.setIncludeDeleted(includeDeleted);
        search.setCreatedFrom(createdFrom);
        search.setCreatedTo(createdTo);

        return ResponseEntity.ok(
                ApiResponse.<PageResponse<UserResponseDto>>builder()
                        .success(true)
                        .code(200)
                        .message("Tìm kiếm thành công")
                        .data(userService.searchUsers(search, pageNo, pageSize))
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
                        .message("Lấy user theo id thành công")
                        .data(userService.getUserById(id))
                        .build()
        );
    }

    /**
     * UC08: Tạo user mới theo SRS.
     * Admin nhập thông tin, hệ thống tự sinh mật khẩu + gửi email kích hoạt.
     */
    @PreAuthorize("hasAuthority('USER:CREATE')")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<CreateUserResponse>builder()
                        .success(true)
                        .code(201)
                        .message("Tạo user thành công")
                        .data(userService.createUser(request))
                        .build()
        );
    }


    @GetMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(@RequestParam String token) {
        userService.activateUser(token);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .code(200)
                        .message("Tài khoản đã được kích hoạt thành công. Bạn có thể đăng nhập.")
                        .build()
        );
    }

    /**
     * UC08-E3: Gửi lại email kích hoạt khi SMTP thất bại lần đầu.
     */
    @PreAuthorize("hasAuthority('USER:CREATE')")
    @PostMapping("/{id}/resend-activation")
    public ResponseEntity<ApiResponse<Void>> resendActivation(@PathVariable Long id) {
        userService.resendActivationEmail(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .code(200)
                        .message("Email kích hoạt đã được gửi lại thành công.")
                        .build()
        );
    }

    /**
     * UC09: Cập nhật thông tin user.
     * BR-019: Không cho phép đổi email và username.
     */
    @PreAuthorize("hasAuthority('USER:UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .code(200)
                        .message("Cập nhật user thành công")
                        .data(userService.updateUser(id, request))
                        .build()
        );
    }

    /**
     * UC11: Xóa user (soft delete).
     * Yêu cầu reason và confirmName. Admin không thể tự xóa mình.
     */
    @PreAuthorize("hasAuthority('USER:DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            @Valid @RequestBody DeleteUserRequest request,
            Authentication authentication) {
        userService.deleteUser(id, request, authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .code(200)
                        .message("Xóa user thành công")
                        .data(null)
                        .build()
        );
    }

    /**
     * UC11-A1: Khôi phục user đã bị soft-delete trong vòng 30 ngày.
     */
    @PreAuthorize("hasAuthority('USER:DELETE')")
    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<UserResponse>> restoreUser(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .code(200)
                        .message("Khôi phục user thành công")
                        .data(userService.restoreUser(id))
                        .build()
        );
    }

    // ========================= LOCK/UNLOCK =============================

    /**
     * UC10: Khóa tài khoản user. Admin không thể tự khóa mình.
     */
    @PreAuthorize("hasAuthority('LOCK:USER')")
    @PatchMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<UserResponse>> lockUser(
            @PathVariable Long id,
            @Valid @RequestBody LockedRequest request,
            Authentication authentication) {
        UserResponse response = userService.lockUser(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .code(200)
                .message("Khóa tài khoản thành công")
                .data(response)
                .build());
    }

    @PreAuthorize("hasAuthority('UNLOCK:USER')")
    @PatchMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(@PathVariable Long id) {
        UserResponse response = userService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .code(200)
                .message("Mở khóa tài khoản thành công")
                .data(response)
                .build());
    }


    // ======================== ASSIGN/REVOKE ROLE ========================

    @PreAuthorize("hasAuthority('ROLE:UPDATE')")
    @PostMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoles(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .code(200)
                        .message("Gán role thành công")
                        .data(userService.assignRoles(userId, request.getRoleIds()))
                        .build());
    }

    @PreAuthorize("hasAuthority('ROLE:UPDATE')")
    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<ApiResponse<UserResponse>> revokeRole(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .code(200)
                        .message("Thu hồi role thành công")
                        .data(userService.revokeRole(userId, request.getRoleIds()))
                        .build()
        );
    }
    @PreAuthorize("hasAuthority('USER:READ')")
    @GetMapping("/students")
    public ResponseEntity<ApiResponse<Object>> getAllStudent(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {

        return ResponseEntity.ok(ApiResponse
                .builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách student thành công")
                .data(userService.getAllStudent(pageNo, pageSize))
                .build());
    }
    // ======================== EXPORT (UC13) ========================
    /**
     * UC12: Import users từ file Excel/CSV
     */
    @PreAuthorize("hasAuthority('USER:CREATE')")
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<ImportUserResponse>> importUsers(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sendActivationEmail", defaultValue = "true")
            boolean sendActivationEmail,
            @RequestParam(value = "dryRun", defaultValue = "false")
            boolean dryRun,
            @RequestParam(value = "defaultRole", defaultValue = "STUDENT")
            String defaultRole) {

        // --- Validate file ---
        if (file.isEmpty()) {
            throw new RuntimeException("File không được để trống");
        }

        // Validate định dạng
        String filename = file.getOriginalFilename();
        if (filename == null ||
                !(filename.endsWith(".xlsx") || filename.endsWith(".xls")
                        || filename.endsWith(".csv"))) {
            throw new RuntimeException("Chỉ hỗ trợ .xlsx, .xls, .csv");
        }

        // Validate kích thước (5MB = 5 * 1024 * 1024)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException(
                    "File vượt quá giới hạn 5MB. Vui lòng chia nhỏ");
        }

        // Validate defaultRole
        if (!List.of("STUDENT", "TEACHER", "ADMIN")
                .contains(defaultRole.toUpperCase())) {
            throw new RuntimeException("Role mặc định không hợp lệ");
        }

        // --- Gọi service ---
        ImportUserResponse result = userImportService.importUsers(
                file, dryRun, sendActivationEmail, defaultRole);

        return ResponseEntity.ok(
                ApiResponse.<ImportUserResponse>builder()
                        .success(true)
                        .code(200)
                        .message(dryRun
                                ? "Dry-run hoàn tất. Không có user nào được tạo."
                                : "Import hoàn tất")
                        .data(result)
                        .build()
        );
    }

    /**
     * UC13: Export danh sách user ra Excel/CSV
     */
    @PreAuthorize("hasAuthority('USER:READ')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(value = "format", defaultValue = "xlsx") String format,
            @RequestParam(value = "includeDeleted", defaultValue = "false")
            boolean includeDeleted,
            @RequestParam(value = "fields", required = false) List<String> fields,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        // Validate format
        if (!format.equals("xlsx") && !format.equals("csv")) {
            throw new RuntimeException("Định dạng không hợp lệ. Chỉ hỗ trợ xlsx, csv");
        }
        // Build request DTO
        ExportUserRequest request = new ExportUserRequest();
        request.setFormat(format);
        request.setIncludeDeleted(includeDeleted);
        request.setFields(fields);
        request.setRole(role);
        request.setStatus(status);
        request.setKeyword(keyword);
        byte[] fileContent = userExportService.exportUsers(request);
        // Xác định Content-Type và tên file
        String contentType;
        String filename;
        if ("csv".equals(format)) {
            contentType = "text/csv";
            filename = "users_export.csv";
        } else {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            filename = "users_export.xlsx";
        }
        // TODO: Ghi audit log (BR-027)
        // auditLogService.log("user:export", adminId, filters, users.size());
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + filename)
                .header("Content-Type", contentType)
                .body(fileContent);
    }
}