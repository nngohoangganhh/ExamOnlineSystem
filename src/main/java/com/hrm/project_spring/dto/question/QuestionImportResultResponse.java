package com.hrm.project_spring.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** DTO trả về kết quả import câu hỏi từ Excel (UC19). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionImportResultResponse {
    private int imported;
    private List<String> errors;
}
