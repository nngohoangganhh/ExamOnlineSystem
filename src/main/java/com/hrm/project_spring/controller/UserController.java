package com.hrm.project_spring.controller;

import com.hrm.project_spring.dto.common.ApiResponse;
import com.hrm.project_spring.dto.common.PageResponse;
import com.hrm.project_spring.dto.user.UserRequest;
import com.hrm.project_spring.dto.user.UserResponse;
import com.hrm.project_spring.dto.user.UserResponseDto;
import com.hrm.project_spring.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.hrm.project_spring.dto.user.ImportUserResponse;
import com.hrm.project_spring.service.UserImportService;
import org.springframework.web.multipart.MultipartFile;
import com.hrm.project_spring.dto.user.ExportUserRequest;
import com.hrm.project_spring.service.UserExportService;
import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserImportService userImportService;
    private final UserExportService userExportService;

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

    @PreAuthorize("hasAuthority('USER:CREATE')")
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        // Tạo file Excel mẫu với header
        try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Users");
            Row header = sheet.createRow(0);
            String[] columns = {"username", "email", "password", "fullName", "role"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                // Style header cho đẹp (tùy chọn)
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }
            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header("Content-Disposition",
                            "attachment; filename=import_users_template.xlsx")
                    .header("Content-Type",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo template: " + e.getMessage());
        }
    }

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
