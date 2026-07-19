package com.hrm.project_spring.service.user;

import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserExportService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public byte[] exportUsers() {
        List<User> users = userRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Users");

            createHeader(sheet);
            writeUserData(sheet, users);
            autoSizeColumns(sheet, 6);

            workbook.write(outputStream);

            return outputStream.toByteArray();

        } catch (IOException exception) {
            throw new RuntimeException("Không thể tạo file Excel danh sách user", exception);
        }
    }

    private void createHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);

        String[] headers = {"ID", "USERNAME", "FULL_NAME", "EMAIL", "STATUS", "CREATED_AT"};

        for (int index = 0; index < headers.length; index++) {
            headerRow.createCell(index).setCellValue(headers[index]);
        }
    }

    private void writeUserData(Sheet sheet, List<User> users) {
        int rowIndex = 1;

        for (User user : users) {
            Row row = sheet.createRow(rowIndex++);

            row.createCell(0).setCellValue(user.getId());

            row.createCell(1).setCellValue(safe(user.getUsername()));

            row.createCell(2).setCellValue(safe(user.getFullName()));

            row.createCell(3).setCellValue(safe(user.getEmail()));

            row.createCell(4).setCellValue(user.getStatus() == null ? "" : user.getStatus().name());

            row.createCell(5).setCellValue(user.getCreatedAt() == null ? "" : user.getCreatedAt().toString());
        }
    }

    private void autoSizeColumns(Sheet sheet, int totalColumns) {
        for (int index = 0; index < totalColumns; index++) {
            sheet.autoSizeColumn(index);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}