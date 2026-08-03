package com.hrm.project_spring.service.question;

import com.hrm.project_spring.entity.Question;
import com.hrm.project_spring.enums.QuestionStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * UC24: Dynamic filter câu hỏi theo nhiều tiêu chí.
 * Dùng với JpaSpecificationExecutor để tránh viết nhiều query method.
 */
public class QuestionSpecification implements Specification<Question> {

    private final Long subjectId;
    private final Long chapterId;
    private final Integer bloomLevel;
    private final QuestionStatus status;
    private final String keyword;
    private final String tag;
    private final Long createdById;
    /** Nếu true, chỉ trả câu hỏi chưa bị soft-delete. */
    private final boolean excludeDeleted;

    public QuestionSpecification(Long subjectId, Long chapterId, Integer bloomLevel,
                                  QuestionStatus status, String keyword, String tag,
                                  Long createdById, boolean excludeDeleted) {
        this.subjectId = subjectId;
        this.chapterId = chapterId;
        this.bloomLevel = bloomLevel;
        this.status = status;
        this.keyword = keyword;
        this.tag = tag;
        this.createdById = createdById;
        this.excludeDeleted = excludeDeleted;
    }

    /** Factory method — tất cả filter null, chỉ excludeDeleted = true. */
    public static QuestionSpecification of(Long subjectId, Long chapterId, Integer bloomLevel,
                                            QuestionStatus status, String keyword, String tag,
                                            Long createdById) {
        return new QuestionSpecification(subjectId, chapterId, bloomLevel, status,
                keyword, tag, createdById, true);
    }

    @Override
    public Predicate toPredicate(Root<Question> root, CriteriaQuery<?> query,
                                  CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // Luôn loại trừ câu hỏi đã bị soft delete
        if (excludeDeleted) {
            predicates.add(cb.isNull(root.get("deletedAt")));
        }

        if (subjectId != null) {
            predicates.add(cb.equal(root.get("subject").get("id"), subjectId));
        }

        if (chapterId != null) {
            predicates.add(cb.equal(root.get("chapter").get("id"), chapterId));
        }

        if (bloomLevel != null) {
            predicates.add(cb.equal(root.get("bloomLevel"), bloomLevel));
        }

        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }

        if (keyword != null && !keyword.isBlank()) {
            String likePattern = "%" + keyword.toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("stem")), likePattern));
        }

        if (tag != null && !tag.isBlank()) {
            Join<Object, Object> tagJoin = root.join("tags", JoinType.INNER);
            predicates.add(cb.equal(cb.lower(tagJoin.get("name")), tag.toLowerCase()));
        }

        if (createdById != null) {
            predicates.add(cb.equal(root.get("createdBy").get("id"), createdById));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
