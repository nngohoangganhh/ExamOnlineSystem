package com.hrm.project_spring.service;

import com.hrm.project_spring.dto.answer.AnswerRequest;
import com.hrm.project_spring.dto.answer.AnswerResponse;
import com.hrm.project_spring.entity.Answer;
import com.hrm.project_spring.entity.Question;
import com.hrm.project_spring.enums.QuestionType;
import com.hrm.project_spring.exception.BadRequestException;
import com.hrm.project_spring.repository.AnswerRepository;
import com.hrm.project_spring.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public List<AnswerResponse> getAnswersByQuestionId(Long questionId, boolean includeIsCorrect) {
        return answerRepository.findByQuestionId(questionId)
                .stream()
                .map(answer -> mapToResponse(answer, includeIsCorrect))
                .collect(Collectors.toList());
    }

    @Transactional
    public AnswerResponse addAnswerToQuestion(Long questionId, AnswerRequest request) {
        Question question = getQuestionById(questionId);

        // Validation: Maximum 4 answers
        if (question.getAnswers().size() >= 4) {
            throw new BadRequestException("Một câu hỏi chỉ được tối đa 4 đáp án.");
        }

        // Validation: Duplicate content
        boolean isDuplicate = question.getAnswers().stream()
                .anyMatch(a -> a.getContent().equalsIgnoreCase(request.getContent()));
        if (isDuplicate) {
            throw new BadRequestException("Nội dung đáp án đã tồn tại trong câu hỏi này.");
        }

        // Validation: Single correct answer constraint (dùng enum QuestionType)
        if (isSingleChoice(question.getType()) && request.getIsCorrect()) {
            boolean hasCorrectAnswer = question.getAnswers().stream().anyMatch(Answer::getIsCorrect);
            if (hasCorrectAnswer) {
                throw new BadRequestException("Câu hỏi này chỉ cho phép một đáp án đúng duy nhất.");
            }
        }

        Answer answer = Answer.builder()
                .content(request.getContent())
                .isCorrect(request.getIsCorrect())
                .question(question)
                .build();

        // Thêm vào danh sách answers của question
        question.getAnswers().add(answer);
        Answer savedAnswer = answerRepository.save(answer);
        return mapToResponse(savedAnswer, true);
    }

    @Transactional
    public List<AnswerResponse> addBulkAnswers(Long questionId, List<AnswerRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("Danh sách đáp án không được để trống.");
        }

        Question question = getQuestionById(questionId);
        int currentSize = question.getAnswers().size();

        // Validation: Maximum 4 answers total
        if (currentSize + requests.size() > 4) {
            throw new BadRequestException("Một câu hỏi chỉ được tối đa 4 đáp án. Hiện tại đã có " + currentSize + " đáp án.");
        }

        // Validation: Duplicate content in requests
        long distinctRequested = requests.stream()
                .map(r -> r.getContent().trim().toLowerCase())
                .distinct()
                .count();
        if (distinctRequested < requests.size()) {
            throw new BadRequestException("Danh sách đáp án gửi lên có nội dung bị trùng lặp.");
        }

        // Validation: Duplicate content with existing answers
        for (AnswerRequest req : requests) {
            boolean exists = question.getAnswers().stream()
                    .anyMatch(a -> a.getContent().equalsIgnoreCase(req.getContent().trim()));
            if (exists) {
                throw new BadRequestException("Đáp án '" + req.getContent() + "' đã tồn tại trong hệ thống.");
            }
        }

        long existingCorrect = question.getAnswers().stream()
                .filter(Answer::getIsCorrect)
                .count();
        long newCorrect = requests.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                .count();

        // Validation: Single choice constraint (dùng enum QuestionType)
        if (isSingleChoice(question.getType())) {
            if (existingCorrect + newCorrect > 1) {
                throw new BadRequestException("Câu hỏi trắc nghiệm này chỉ được phép có 1 đáp án đúng.");
            }
        }

        if (currentSize + requests.size() == 4 && (existingCorrect + newCorrect == 0)) {
            throw new BadRequestException("Câu hỏi phải có ít nhất một đáp án đúng.");
        }

        List<Answer> newAnswers = requests.stream()
                .map(req -> {
                    Answer ans = Answer.builder()
                            .content(req.getContent().trim())
                            .isCorrect(req.getIsCorrect())
                            .question(question)
                            .build();
                    question.getAnswers().add(ans);
                    return ans;
                })
                .collect(Collectors.toList());

        List<Answer> savedAnswers = answerRepository.saveAll(newAnswers);
        return savedAnswers.stream().map(a -> mapToResponse(a, true)).collect(Collectors.toList());
    }

    @Transactional
    public AnswerResponse updateAnswer(Long questionId, Long answerId, AnswerRequest request) {
        Answer answer = answerRepository.findByIdAndQuestionId(answerId, questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy nội dung đáp án."));

        Question question = answer.getQuestion();

        // Validation: Duplicate content
        boolean isDuplicate = question.getAnswers().stream()
                .anyMatch(a -> !a.getId().equals(answerId) && a.getContent().equalsIgnoreCase(request.getContent()));
        if (isDuplicate) {
            throw new BadRequestException("Nội dung đáp án này đã tồn tại trong câu hỏi.");
        }

        // Validation: Single correct answer constraint
        if (isSingleChoice(question.getType()) && request.getIsCorrect() && !Boolean.TRUE.equals(answer.getIsCorrect())) {
            boolean hasOtherCorrectAnswer = question.getAnswers().stream()
                    .anyMatch(a -> a.getIsCorrect() && !a.getId().equals(answerId));
            if (hasOtherCorrectAnswer) {
                throw new BadRequestException("Câu hỏi này chỉ cho phép một đáp án đúng duy nhất.");
            }
        }

        answer.setContent(request.getContent().trim());
        answer.setIsCorrect(request.getIsCorrect());

        return mapToResponse(answerRepository.save(answer), true);
    }

    @Transactional
    public void deleteAnswer(Long questionId, Long answerId) {
        Answer answer = answerRepository.findByIdAndQuestionId(answerId, questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy câu trả lời"));
        // Gỡ ra khỏi danh sách của question trước khi xóa
        answer.getQuestion().getAnswers().remove(answer);
        answerRepository.delete(answer);
    }

    // ======================== HELPERS ========================

    private boolean isSingleChoice(QuestionType type) {
        if (type == null) return false;
        return type == QuestionType.MCQ_SINGLE || type == QuestionType.TRUE_FALSE;
    }

    private Question getQuestionById(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có câu hỏi phù hợp"));
    }

    private AnswerResponse mapToResponse(Answer answer, boolean includeIsCorrect) {
        return AnswerResponse.builder()
                .id(answer.getId())
                .questionId(answer.getQuestion().getId())
                .content(answer.getContent())
                .isCorrect(includeIsCorrect ? answer.getIsCorrect() : null)
                .build();
    }
}
