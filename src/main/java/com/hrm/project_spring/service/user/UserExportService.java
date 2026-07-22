package com.hrm.project_spring.service.user;

import com.hrm.project_spring.entity.ClassRoom;
import com.hrm.project_spring.entity.Role;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.UserStatus;
import com.hrm.project_spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UC13: Export danh sách user ra Excel hoặc CSV.
 * Hỗ trợ filter theo status, roleId, includeDeleted.
 * Giới hạn 50.000 dòng để tránh OOM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserExportService {

    private static final int MAX_EXPORT_ROWS = 50_000;

    private final UserRepository userRepository;

    /**
     * Export sang xlsx.
     *
     * @param status         Lọc theo trạng thái (null = tất cả)
     * @param roleId         Lọc theo role id (null = tất cả)
     * @param includeDeleted Có bao gồm user đã soft-delete không
     */
    @Transactional(readOnly = true)
    public byte[] exportUsersXlsx(UserStatus status, Long roleId, boolean includeDeleted) {
        List<User> users = getFilteredUsers(status, roleId, includeDeleted);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Style cho header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet sheet = workbook.createSheet("Users");
            createHeader(sheet, headerStyle);
            writeUserData(sheet, users);
            autoSizeColumns(sheet, HEADERS.length);

            workbook.write(outputStream);
            log.info("UC13: Export {} users (status={}, roleId={}, includeDeleted={})",
                    users.size(), status, roleId, includeDeleted);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo file Excel danh sách user", e);
        }
    }

    /**
     * Export sang CSV.
     */
    @Transactional(readOnly = true)
    public byte[] exportUsersCsv(UserStatus status, Long roleId, boolean includeDeleted) {
        List<User> users = getFilteredUsers(status, roleId, includeDeleted);

        StringBuilder sb = new StringBuilder();
        // Header
        sb.append(String.join(",", HEADERS)).append("\n");
        // Rows
        for (User user : users) {
            sb.append(toCsvRow(user)).append("\n");
        }
        log.info("UC13: Export CSV {} users", users.size());
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ======================== PRIVATE HELPERS ========================

    private static final String[] HEADERS = {
            "ID", "USERNAME", "FULL_NAME", "EMAIL", "PHONE", "BIRTH_DATE",
            "GENDER", "STUDENT_CODE", "EMPLOYEE_CODE", "STATUS",
            "ROLES", "CLASS_CODES", "CREATED_AT"
    };

    private List<User> getFilteredUsers(UserStatus status, Long roleId, boolean includeDeleted) {
        List<User> users = userRepository.findAllForExport(status, roleId, includeDeleted);

        // UC13: Giới hạn 50.000 dòng
        if (users.size() > MAX_EXPORT_ROWS) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Kết quả vượt quá " + MAX_EXPORT_ROWS + " dòng. Hãy áp dụng bộ lọc để thu hẹp phạm vi.");
        }
        return users;
    }

    private void createHeader(Sheet sheet, CellStyle style) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(style);
        }
    }

    private void writeUserData(Sheet sheet, List<User> users) {
        int rowIndex = 1;
        for (User user : users) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(user.getId() != null ? user.getId() : 0L);
            row.createCell(1).setCellValue(safe(user.getUsername()));
            row.createCell(2).setCellValue(safe(user.getFullName()));
            row.createCell(3).setCellValue(safe(user.getEmail()));
            row.createCell(4).setCellValue(safe(user.getPhone()));
            row.createCell(5).setCellValue(user.getBirthDate() != null ? user.getBirthDate().toString() : "");
            row.createCell(6).setCellValue(user.getGender() != null ? user.getGender().name() : "");
            row.createCell(7).setCellValue(safe(user.getStudentCode()));
            row.createCell(8).setCellValue(safe(user.getEmployeeCode()));
            row.createCell(9).setCellValue(user.getStatus() != null ? user.getStatus().name() : "");
            row.createCell(10).setCellValue(getRoleNames(user));
            row.createCell(11).setCellValue(getClassCodes(user));
            row.createCell(12).setCellValue(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        }
    }

    private String toCsvRow(User user) {
        return String.join(",",
                csvEscape(user.getId() != null ? String.valueOf(user.getId()) : ""),
                csvEscape(user.getUsername()),
                csvEscape(user.getFullName()),
                csvEscape(user.getEmail()),
                csvEscape(user.getPhone()),
                csvEscape(user.getBirthDate() != null ? user.getBirthDate().toString() : ""),
                csvEscape(user.getGender() != null ? user.getGender().name() : ""),
                csvEscape(user.getStudentCode()),
                csvEscape(user.getEmployeeCode()),
                csvEscape(user.getStatus() != null ? user.getStatus().name() : ""),
                csvEscape(getRoleNames(user)),
                csvEscape(getClassCodes(user)),
                csvEscape(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "")
        );
    }

    private String getRoleNames(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) return "";
        return user.getRoles().stream().map(Role::getCode).collect(Collectors.joining("|"));
    }

    private String getClassCodes(User user) {
        if (user.getClassRooms() == null || user.getClassRooms().isEmpty()) return "";
        return user.getClassRooms().stream().map(ClassRoom::getCode).collect(Collectors.joining("|"));
    }

    private void autoSizeColumns(Sheet sheet, int totalColumns) {
        for (int i = 0; i < totalColumns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        // Escape dấu phẩy, nháy kép và xuống dòng
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}