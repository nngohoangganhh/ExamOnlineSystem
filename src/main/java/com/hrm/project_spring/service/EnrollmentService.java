package com.hrm.project_spring.service;

import com.hrm.project_spring.entity.*;
import com.hrm.project_spring.enums.AuditAction;
import com.hrm.project_spring.exception.BadRequestException;
import com.hrm.project_spring.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UC30: Gán thí sinh / lớp học vào bài thi.
 * BR-051: Mỗi enrollment = maxAttempts lượt.
 * BR-052: Xóa enrollment không xóa kết quả cũ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final ClassRoomRepository classRoomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditLogService auditLogService;

    /**
     * UC30: Gán thí sinh theo danh sách userId vào bài thi.
     */
    @Transactional
    public int enrollUsers(Long testId, List<Long> userIds, HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User assigner = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng."));

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BadRequestException("Bài thi không tồn tại."));

        int enrolled = 0;
        for (Long userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BadRequestException("User " + userId + " không tồn tại."));

            if (enrollmentRepository.existsByTestAndUser(test, user)) {
                log.warn("UC30: User {} đã enrolled vào test {}, bỏ qua.", userId, testId);
                continue;
            }

            enrollmentRepository.save(Enrollment.builder()
                    .test(test)
                    .user(user)
                    .attemptsUsed(0)
                    .assignedBy(assigner)
                    .build());
            enrolled++;
        }

        auditLogService.log(assigner.getId(), username, AuditAction.TEST_UPDATE, request,
                "{\"testId\":" + testId + ",\"enrolledUsers\":" + enrolled + "}");
        return enrolled;
    }

    /**
     * UC30: Gán cả lớp học vào bài thi.
     * Lấy tất cả sinh viên trong lớp rồi enroll từng người.
     */
    @Transactional
    public int enrollClassRoom(Long testId, Long classRoomId, HttpServletRequest request) {
        ClassRoom classRoom = classRoomRepository.findById(classRoomId)
                .orElseThrow(() -> new BadRequestException("Lớp học không tồn tại."));

        Set<User> students = classRoom.getStudents();
        if (students.isEmpty()) {
            throw new BadRequestException("Lớp học không có sinh viên.");
        }

        List<Long> userIds = students.stream().map(User::getId).toList();
        return enrollUsers(testId, userIds, request);
    }

    /**
     * BR-052: Xóa enrollment — KHÔNG xóa Attempt đã có.
     * Thí sinh không còn quyền vào thi mới, nhưng kết quả cũ vẫn giữ.
     */
    @Transactional
    public void unenroll(Long testId, Long userId, HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User assigner = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng."));

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BadRequestException("Bài thi không tồn tại."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại."));

        Enrollment enrollment = enrollmentRepository.findByTestAndUser(test, user)
                .orElseThrow(() -> new BadRequestException("Thí sinh chưa được gán vào bài thi này."));

        // BR-052: chỉ xóa enrollment, không xóa Attempt
        enrollmentRepository.delete(enrollment);

        auditLogService.log(assigner.getId(), username, AuditAction.TEST_UPDATE, request,
                "{\"testId\":" + testId + ",\"unenrolledUser\":" + userId + "}");
    }

    /** Lấy danh sách enrollment của một bài thi. */
    @Transactional
    public List<Enrollment> getEnrollments(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BadRequestException("Bài thi không tồn tại."));
        return enrollmentRepository.findAllByTest(test);
    }
}
