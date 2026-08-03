package com.hrm.project_spring.service.question;

import com.hrm.project_spring.entity.*;
import com.hrm.project_spring.enums.AuditAction;
import com.hrm.project_spring.enums.QuestionStatus;
import com.hrm.project_spring.enums.QuestionType;
import com.hrm.project_spring.exception.BadRequestException;
import com.hrm.project_spring.repository.*;
import com.hrm.project_spring.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * UC19: Import câu hỏi từ file Excel.
 * BR-035: Câu hỏi import vào với status = DRAFT.
 *
 * Cấu trúc file Excel (Sheet 1):
 * type | stem | subjectId | chapterId | bloomLevel | score | explanation |
 * referenceAnswer | rubric | options (JSON) | tags (comma-separated)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionImportService {

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ImportResult importFromExcel(MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) throw new BadRequestException("File không được để trống.");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BadRequestException("Chỉ hỗ trợ file .xlsx");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng."));

        List<String> errors = new ArrayList<>();
        List<Question> toSave = new ArrayList<>();
        int rowIndex = 1; // 0 = header

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new BadRequestException("File thiếu header.");

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                rowIndex = r + 1;
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    Question q = parseRow(row, currentUser);
                    toSave.add(q);
                } catch (Exception e) {
                    errors.add("Dòng " + rowIndex + ": " + e.getMessage());
                }
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi đọc file Excel: " + e.getMessage());
        }

        questionRepository.saveAll(toSave);

        auditLogService.log(currentUser.getId(), username, AuditAction.QUESTION_IMPORT, request,
                "{\"file\":\"" + filename + "\",\"imported\":" + toSave.size() +
                ",\"errors\":" + errors.size() + "}");

        log.info("UC19: Import {} câu hỏi, {} lỗi từ {}", toSave.size(), errors.size(), filename);
        return new ImportResult(toSave.size(), errors);
    }

    private Question parseRow(Row row, User currentUser) {
        String typeStr  = getCellString(row, 0);
        String stem     = getCellString(row, 1);
        Long subjectId  = getCellLong(row, 2);
        Long chapterId  = getCellLong(row, 3);
        int bloomLevel  = (int) getCellNumeric(row, 4);
        BigDecimal score = BigDecimal.valueOf(getCellNumeric(row, 5));
        String explanation    = getCellString(row, 6);
        String referenceAnswer = getCellString(row, 7);
        String rubric   = getCellString(row, 8);
        String tagsRaw  = getCellString(row, 10);

        if (typeStr == null || typeStr.isBlank()) throw new BadRequestException("type không được trống");
        QuestionType type;
        try { type = QuestionType.valueOf(typeStr.trim().toUpperCase()); }
        catch (Exception e) { throw new BadRequestException("type không hợp lệ: " + typeStr); }

        if (stem == null || stem.isBlank()) throw new BadRequestException("stem không được trống");
        if (subjectId == null) throw new BadRequestException("subjectId không hợp lệ");
        if (chapterId == null) throw new BadRequestException("chapterId không hợp lệ");

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new BadRequestException("subjectId " + subjectId + " không tồn tại"));
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new BadRequestException("chapterId " + chapterId + " không tồn tại"));
        if (!chapter.getSubject().getId().equals(subject.getId())) {
            throw new BadRequestException("Chương không thuộc môn học");
        }

        // Parse tags
        Set<Tag> tags = new HashSet<>();
        if (tagsRaw != null && !tagsRaw.isBlank()) {
            for (String tagName : tagsRaw.split(",")) {
                String normalized = tagName.trim().toLowerCase().replaceAll("[^a-z0-9-]", "");
                if (normalized.isBlank()) continue;
                Tag tag = tagRepository.findByName(normalized);
                if (tag == null) {
                    tag = tagRepository.save(Tag.builder().name(normalized).build());
                }
                tags.add(tag);
            }
        }

        return Question.builder()
                .type(type)
                .stem(stem)
                .subject(subject)
                .chapter(chapter)
                .bloomLevel(bloomLevel)
                .score(score)
                .explanation(explanation)
                .referenceAnswer(referenceAnswer)
                .rubric(rubric)
                // BR-035: Import vào với status = DRAFT
                .status(QuestionStatus.DRAFT)
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .tags(tags)
                .build();
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private double getCellNumeric(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return 0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING  -> {
                try { yield Double.parseDouble(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield 0; }
            }
            default -> 0;
        };
    }

    private Long getCellLong(Row row, int col) {
        double val = getCellNumeric(row, col);
        return val == 0 ? null : (long) val;
    }

    /** Kết quả import trả về cho controller. */
    public record ImportResult(int imported, List<String> errors) {}
}
