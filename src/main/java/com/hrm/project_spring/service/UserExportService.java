package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.user.ExportUserRequest;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.repository.UserRepository;
import com.hrm.project_spring.repository.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserExportService {

    private final UserRepository userRepository;

    // Tất cả trường hợp lệ có thể xuất
    private static final List<String> ALL_FIELDS = List.of(
            "id", "username", "email", "fullName", "role", "status", "createdAt"
    );

    private static final int MAX_EXPORT_ROWS = 50_000;

    /**
     * Export danh sách user ra byte[] (nội dung file)
     */
    public byte[] exportUsers(ExportUserRequest request) {
        // 1. Validate fields
        List<String> fields = (request.getFields() != null
                && !request.getFields().isEmpty())
                ? request.getFields()
                : ALL_FIELDS; // mặc định xuất hết

        // Kiểm tra field hợp lệ
        List<String> invalidFields = fields.stream()
                .filter(f -> !ALL_FIELDS.contains(f))
                .toList();
        if (!invalidFields.isEmpty()) {
            throw new RuntimeException(
                    "Trường không hợp lệ: " + String.join(", ", invalidFields));
        }

        // 2. Build query specification (filter động)
        Specification<User> spec = Specification.where(null);

        if (!request.isIncludeDeleted()) {
            spec = spec.and(UserSpecification.notDeleted());
        }
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            spec = spec.and(UserSpecification.hasRole(request.getRole()));
        }
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            spec = spec.and(UserSpecification.hasStatus(request.getStatus()));
        }
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            spec = spec.and(UserSpecification.containsKeyword(request.getKeyword()));
        }

        // 3. Query DB
        List<User> users = userRepository.findAll(spec);

        // Giới hạn 50.000 dòng
        if (users.size() > MAX_EXPORT_ROWS) {
            throw new RuntimeException(
                    "Vượt giới hạn " + MAX_EXPORT_ROWS
                            + " dòng, vui lòng thu hẹp filter");
        }

        // 4. Tạo file theo format
        try {
            return switch (request.getFormat().toLowerCase()) {
                case "xlsx" -> generateExcel(users, fields);
                case "csv"  -> generateCsv(users, fields);
                default -> throw new RuntimeException("Định dạng không hợp lệ");
            };
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Hệ thống tạm quá tải, vui lòng thử lại sau");
        }
    }

    /**
     * Tạo file Excel từ danh sách user
     */
    private byte[] generateExcel(List<User> users, List<String> fields)
            throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Users");

            // Header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < fields.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(fields.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int r = 0; r < users.size(); r++) {
                Row row = sheet.createRow(r + 1);
                User user = users.get(r);
                for (int c = 0; c < fields.size(); c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(getFieldValue(user, fields.get(c)));
                }
            }

            // Auto-size
            for (int i = 0; i < fields.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Tạo file CSV từ danh sách user
     */
    private byte[] generateCsv(List<User> users, List<String> fields) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(String.join(",", fields)).append("\n");

        // Data
        for (User user : users) {
            List<String> values = fields.stream()
                    .map(f -> escapeCsv(getFieldValue(user, f)))
                    .toList();
            sb.append(String.join(",", values)).append("\n");
        }

        return sb.toString().getBytes();
    }

    /**
     * Lấy giá trị field từ User entity theo tên field
     */
    private String getFieldValue(User user, String field) {
        return switch (field) {
            case "id"        -> String.valueOf(user.getId());
            case "username"  -> user.getUsername() != null ? user.getUsername() : "";
            case "email"     -> user.getEmail() != null ? user.getEmail() : "";
            case "fullName"  -> user.getFullName() != null ? user.getFullName() : "";
            case "role"      -> {
                if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                    yield user.getRoles().stream()
                            .map(r -> r.getCode())
                            .collect(Collectors.joining(", "));
                }
                yield "";
            }
            case "status"    -> user.getStatus() != null
                    ? user.getStatus().name() : "";
            case "createdAt" -> user.getCreatedAt() != null
                    ? user.getCreatedAt().toString() : "";
            default          -> "";
        };
    }

    /**
     * Escape giá trị CSV (xử lý dấu phẩy, xuống dòng, ngoặc kép)
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}