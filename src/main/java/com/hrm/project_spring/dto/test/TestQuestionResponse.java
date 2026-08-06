package com.hrm.project_spring.dto.test;

import com.hrm.project_spring.enums.QuestionStatus;
import com.hrm.project_spring.enums.QuestionType;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO trả về danh sách câu hỏi trong bài thi.
 * Thay thế việc trả entity TestQuestion thô ra ngoài API.
 * Không expose isCorrect để bảo vệ đáp án đúng với student.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestQuestionResponse {

    /** ID của bản ghi TestQuestion (junction). */
    private Long id;

    /** ID câu hỏi gốc. */
    private Long questionId;

    /** Thứ tự câu hỏi trong đề thi. */
    private Integer orderNum;

    /** Điểm của câu hỏi này trong đề thi. */
    private BigDecimal score;

    /** Nội dung câu hỏi (rút gọn 200 ký tự để hiển thị danh sách). */
    private String stem;

    /** Loại câu hỏi: MCQ_SINGLE, MCQ_MULTIPLE, TRUE_FALSE, ESSAY. */
    private QuestionType type;

    /** Trạng thái câu hỏi: APPROVED, DRAFT, ... */
    private QuestionStatus questionStatus;

    /** Mức độ khó theo thang Bloom (1–6). */
    private Integer bloomLevel;

    /** Tên môn học. */
    private String subjectName;

    /** Tên chương. */
    private String chapterName;
}
