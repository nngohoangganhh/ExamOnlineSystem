package com.hrm.project_spring.service.question;

import com.hrm.project_spring.entity.Question;
import com.hrm.project_spring.entity.User;
import com.hrm.project_spring.enums.AuditAction;
import com.hrm.project_spring.enums.QuestionStatus;
import com.hrm.project_spring.exception.BadRequestException;
import com.hrm.project_spring.repository.QuestionRepository;
import com.hrm.project_spring.repository.UserRepository;
import com.hrm.project_spring.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * UC20: Export câu hỏi ra file Excel.
 * BR-036: Chỉ export câu hỏi đã APPROVED (hoặc theo filter truyền vào).
 * Ghi audit log sau khi export thành công.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionExportService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    private static final String[] HEADERS = {
            "ID", "Type", "Stem", "Subject", "Chapter", "BloomLevel",
            "Score", "Status", "Tags", "Explanation", "ReferenceAnswer", "CreatedBy", "CreatedAt"
    };

    /**
     * Export tất cả câu hỏi APPROVED (hoặc theo Specification) ra XLSX byte array.
     * Caller truyền response header Content-Disposition bên ngoài.
     */
    @Transactional(readOnly = true)
    public byte[] exportToExcel(Long subjectId, Long chapterId,
                                Integer bloomLevel, QuestionStatus status,
                                String keyword, String tag,
                                HttpServletRequest request) {
        // BR-036: default export câu hỏi APPROVED nếu không truyền status
        QuestionStatus exportStatus = (status != null) ? status : QuestionStatus.APPROVED;

        Specification<Question> spec = QuestionSpecification.of(
                subjectId, chapterId, bloomLevel, exportStatus, keyword, tag, null);
        List<Question> questions = questionRepository.findAll(spec);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng."));

        byte[] bytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Questions");

            // Header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Question q : questions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(q.getId());
                row.createCell(1).setCellValue(q.getType() != null ? q.getType().name() : "");
                row.createCell(2).setCellValue(q.getStem());
                row.createCell(3).setCellValue(q.getSubject() != null ? q.getSubject().getName() : "");
                row.createCell(4).setCellValue(q.getChapter() != null ? q.getChapter().getName() : "");
                row.createCell(5).setCellValue(q.getBloomLevel() != null ? q.getBloomLevel() : 0);
                row.createCell(6).setCellValue(q.getScore() != null ? q.getScore().doubleValue() : 0);
                row.createCell(7).setCellValue(q.getStatus() != null ? q.getStatus().name() : "");
                row.createCell(8).setCellValue(q.getTags().stream()
                        .map(t -> t.getName()).reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b));
                row.createCell(9).setCellValue(q.getExplanation() != null ? q.getExplanation() : "");
                row.createCell(10).setCellValue(q.getReferenceAnswer() != null ? q.getReferenceAnswer() : "");
                row.createCell(11).setCellValue(q.getCreatedBy() != null ? q.getCreatedBy().getUsername() : "");
                row.createCell(12).setCellValue(q.getCreatedAt() != null ? q.getCreatedAt().toString() : "");
            }

            // Auto-size columns
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            bytes = out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo file Excel: " + e.getMessage(), e);
        }

        // BR-036: Audit log sau khi export
        auditLogService.log(currentUser.getId(), username, AuditAction.QUESTION_EXPORT, request,
                "{\"exported\":" + questions.size() + ",\"status\":\"" + exportStatus.name() + "\"}");

        log.info("UC20: Exported {} questions by {}", questions.size(), username);
        return bytes;
    }
}
