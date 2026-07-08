package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.user.ImportUserResponse;
import com.hrm.project_spring.dto.user.ImportUserRowResult;
import com.hrm.project_spring.entity.Role;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.UserStatus;
import com.hrm.project_spring.repository.ClassRoomRepository;
import com.hrm.project_spring.repository.RoleRepository;
import com.hrm.project_spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserImportService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ClassRoomRepository classRoomRepository;
    private final PasswordEncoder passwordEncoder;

    // Các cột bắt buộc trong file template
    private static final List<String> REQUIRED_COLUMNS =
            List.of("username", "email", "password", "fullname");

    /**
     * Entry point: xử lý file import
     */
    public ImportUserResponse importUsers(MultipartFile file,
                                          boolean dryRun,
                                          boolean sendActivationEmail,
                                          String defaultRole) {
        // 1. Lấy tên file và extension
        String filename = file.getOriginalFilename();
        String extension = getFileExtension(filename);

        // 2. Đọc dữ liệu từ file thành List<Map<String, String>>
        //    Mỗi Map = 1 dòng, key = tên cột header, value = giá trị ô
        List<Map<String, String>> rows;
        try {
            rows = parseFile(file, extension);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file: " + e.getMessage());
        }

        // 3. Kiểm tra cột bắt buộc
        if (!rows.isEmpty()) {
            Set<String> headers = rows.get(0).keySet();
            List<String> missingCols = REQUIRED_COLUMNS.stream()
                    .filter(col -> !headers.contains(col))
                    .toList();
            if (!missingCols.isEmpty()) {
                throw new RuntimeException(
                        "File thiếu cột: " + String.join(", ", missingCols));
            }
        }

        // 4. Xử lý từng dòng
        List<ImportUserRowResult> results = new ArrayList<>();
        int successCount = 0, skipCount = 0, errorCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 2; // +2 vì dòng 1 là header, index bắt đầu từ 0

            ImportUserRowResult result = processRow(
                    row, rowNum, dryRun, defaultRole);
            results.add(result);

            switch (result.getStatus()) {
                case "success" -> successCount++;
                case "skip"    -> skipCount++;
                case "error"   -> errorCount++;
            }
        }

        // 5. TODO: Nếu sendActivationEmail = true và dryRun = false,
        //    gửi email kích hoạt cho tất cả user vừa tạo (BR-026: gửi batch)
        //    Bạn có thể gọi EmailService ở đây

        return ImportUserResponse.builder()
                .totalRows(rows.size())
                .successCount(successCount)
                .skipCount(skipCount)
                .errorCount(errorCount)
                .dryRun(dryRun)
                .details(results)
                .build();
    }

    /**
     * Xử lý 1 dòng dữ liệu
     */
    private ImportUserRowResult processRow(Map<String, String> row,
                                           int rowNum,
                                           boolean dryRun,
                                           String defaultRole) {
        String username = trim(row.get("username"));
        String email    = trim(row.get("email"));
        String password = trim(row.get("password"));
        String fullName = trim(row.get("fullname"));
        String roleName = trim(row.get("role")); // cột role (không bắt buộc)

        // --- Validate ---
        if (username == null || username.isEmpty()) {
            return errorResult(rowNum, username, email, "Username không được để trống");
        }
        if (email == null || email.isEmpty()) {
            return errorResult(rowNum, username, email, "Email không được để trống");
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$")) {
            return errorResult(rowNum, username, email, "Email không hợp lệ");
        }
        if (password == null || password.length() < 6) {
            return errorResult(rowNum, username, email,
                    "Password phải có ít nhất 6 ký tự");
        }

        // --- Kiểm tra trùng ---
        if (userRepository.existsByUsername(username)) {
            return ImportUserRowResult.builder()
                    .rowNumber(rowNum).username(username).email(email)
                    .status("skip")
                    .message("Username đã tồn tại")
                    .build();
        }
        if (userRepository.existsByEmail(email)) {
            return ImportUserRowResult.builder()
                    .rowNumber(rowNum).username(username).email(email)
                    .status("skip")
                    .message("Email đã tồn tại")
                    .build();
        }

        // --- Xác định Role ---
        String roleCode = (roleName != null && !roleName.isEmpty())
                ? roleName.toUpperCase()
                : defaultRole.toUpperCase();

        // Validate role hợp lệ
        if (!List.of("STUDENT", "TEACHER", "ADMIN").contains(roleCode)) {
            return errorResult(rowNum, username, email,
                    "Role không hợp lệ: " + roleCode +
                            ". Chỉ chấp nhận: STUDENT, TEACHER, ADMIN");
        }

        Optional<Role> roleOpt = roleRepository.findByCode(roleCode);
        if (roleOpt.isEmpty()) {
            return errorResult(rowNum, username, email,
                    "Role '" + roleCode + "' không tồn tại trong DB");
        }

        // --- Dry-run: chỉ validate, không tạo ---
        if (dryRun) {
            return ImportUserRowResult.builder()
                    .rowNumber(rowNum).username(username).email(email)
                    .status("success")
                    .message("(dry-run) Dữ liệu hợp lệ, sẵn sàng tạo")
                    .build();
        }

        // --- Tạo user thật ---
        try {
            User user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .email(email)
                    .fullName(fullName)
                    .status(UserStatus.ACTIVE)
                    .roles(Set.of(roleOpt.get()))
                    .build();
            userRepository.save(user);

            return ImportUserRowResult.builder()
                    .rowNumber(rowNum).username(username).email(email)
                    .status("success")
                    .message("Tạo user thành công")
                    .build();
        } catch (Exception e) {
            return errorResult(rowNum, username, email,
                    "Lỗi hệ thống: " + e.getMessage());
        }
    }

    // ========== Phương thức đọc file ==========

    /**
     * Đọc file và trả về danh sách các dòng (mỗi dòng = Map<tên_cột, giá_trị>)
     */
    private List<Map<String, String>> parseFile(MultipartFile file,
                                                String extension)
            throws Exception {
        return switch (extension) {
            case "xlsx" -> parseExcel(file, false);
            case "xls"  -> parseExcel(file, true);
            case "csv"  -> parseCsv(file);
            default -> throw new RuntimeException(
                    "Định dạng không hỗ trợ: " + extension);
        };
    }

    /**
     * Đọc file Excel (.xlsx hoặc .xls)
     */
    private List<Map<String, String>> parseExcel(MultipartFile file,
                                                 boolean isXls)
            throws Exception {
        List<Map<String, String>> result = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = isXls
                     ? new HSSFWorkbook(is)     // .xls (Excel 97-2003)
                     : new XSSFWorkbook(is)) {  // .xlsx (Excel 2007+)

            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên

            // Đọc header (dòng 0)
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("File rỗng hoặc thiếu header");
            }

            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                headers.add(getCellStringValue(cell).trim().toLowerCase());
            }

            // Đọc từng dòng dữ liệu (bắt đầu từ dòng 1)
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue; // bỏ qua dòng trống

                Map<String, String> map = new LinkedHashMap<>();
                boolean hasData = false;
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c);
                    String value = getCellStringValue(cell);
                    map.put(headers.get(c), value);
                    if (value != null && !value.isEmpty()) {
                        hasData = true;
                    }
                }
                if (hasData) { // Chỉ thêm dòng có dữ liệu
                    result.add(map);
                }
            }
        }
        return result;
    }

    /**
     * Đọc file CSV
     */
    private List<Map<String, String>> parseCsv(MultipartFile file)
            throws Exception {
        List<Map<String, String>> result = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> allRows = csvReader.readAll();
            if (allRows.isEmpty()) {
                throw new RuntimeException("File CSV rỗng");
            }

            // Dòng đầu = header
            String[] headerArr = allRows.get(0);
            List<String> headers = Arrays.stream(headerArr)
                    .map(h -> h.trim().toLowerCase())
                    .toList();

            // Các dòng còn lại = data
            for (int i = 1; i < allRows.size(); i++) {
                String[] rowData = allRows.get(i);
                Map<String, String> map = new LinkedHashMap<>();
                boolean hasData = false;
                for (int c = 0; c < headers.size(); c++) {
                    String value = (c < rowData.length) ? rowData[c].trim() : "";
                    map.put(headers.get(c), value);
                    if (!value.isEmpty()) hasData = true;
                }
                if (hasData) result.add(map);
            }
        }
        return result;
    }

    // ========== Utility methods ==========

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new RuntimeException("Tên file không hợp lệ");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Đọc giá trị cell Excel ra String
     * (Cell có thể chứa số, text, boolean, formula...)
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> {
                // Tránh trường hợp Excel lưu "123" thành 123.0
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getStringCellValue();
            default      -> "";
        };
    }

    private String trim(String s) {
        return (s == null) ? null : s.trim();
    }

    private ImportUserRowResult errorResult(int rowNum, String username,
                                            String email, String message) {
        return ImportUserRowResult.builder()
                .rowNumber(rowNum)
                .username(username)
                .email(email)
                .status("error")
                .message(message)
                .build();
    }
}