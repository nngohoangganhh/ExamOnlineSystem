package com.hrm.project_spring.config;

import com.hrm.project_spring.entity.*;
import com.hrm.project_spring.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Tự động seed dữ liệu mặc định khi ứng dụng khởi động.
 * Chỉ tạo nếu chưa tồn tại (idempotent - chạy nhiều lần không bị lỗi).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final FeatureRepository featureRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== DataInitializer: Seeding default data ===");

        // 1. Tạo Features
        Feature userFeature      = createFeatureIfNotExists("USER",       "Quản lý người dùng");
        Feature roleFeature      = createFeatureIfNotExists("ROLE",       "Quản lý vai trò");
        Feature examFeature      = createFeatureIfNotExists("EXAM",       "Quản lý kỳ thi");
        Feature testFeature      = createFeatureIfNotExists("TEST",       "Quản lý bài thi");
        Feature featureFeature   = createFeatureIfNotExists("FEATURE",    "Quản lý tính năng");
        Feature permFeature      = createFeatureIfNotExists("PERMISSION", "Quản lý phân quyền");
        Feature questionFeature  = createFeatureIfNotExists("QUESTION",   "Quản lý câu hỏi");

        // 2. Tạo Permissions
        Permission userRead    = createPermIfNotExists("USER:READ",    "READ",   "Xem người dùng",    userFeature);
        Permission userCreate  = createPermIfNotExists("USER:CREATE",  "CREATE", "Tạo người dùng",    userFeature);
        Permission userUpdate  = createPermIfNotExists("USER:UPDATE",  "UPDATE", "Sửa người dùng",    userFeature);
        Permission userDelete  = createPermIfNotExists("USER:DELETE",  "DELETE", "Xóa người dùng",    userFeature);

        Permission roleRead    = createPermIfNotExists("ROLE:READ",    "READ",   "Xem vai trò",       roleFeature);
        Permission roleCreate  = createPermIfNotExists("ROLE:CREATE",  "CREATE", "Tạo vai trò",       roleFeature);
        Permission roleUpdate  = createPermIfNotExists("ROLE:UPDATE",  "UPDATE", "Sửa vai trò",       roleFeature);
        Permission roleDelete  = createPermIfNotExists("ROLE:DELETE",  "DELETE", "Xóa vai trò",       roleFeature);

        Permission examRead    = createPermIfNotExists("EXAM:READ",    "READ",   "Xem kỳ thi",        examFeature);
        Permission examCreate  = createPermIfNotExists("EXAM:CREATE",  "CREATE", "Tạo kỳ thi",        examFeature);
        Permission examUpdate  = createPermIfNotExists("EXAM:UPDATE",  "UPDATE", "Sửa kỳ thi",        examFeature);
        Permission examDelete  = createPermIfNotExists("EXAM:DELETE",  "DELETE", "Xóa kỳ thi",        examFeature);

        Permission testRead    = createPermIfNotExists("TEST:READ",    "READ",   "Xem bài thi",       testFeature);
        Permission testCreate  = createPermIfNotExists("TEST:CREATE",  "CREATE", "Tạo bài thi",       testFeature);
        Permission testUpdate  = createPermIfNotExists("TEST:UPDATE",  "UPDATE", "Sửa bài thi",       testFeature);
        Permission testDelete  = createPermIfNotExists("TEST:DELETE",  "DELETE", "Xóa bài thi",       testFeature);

        Permission featRead    = createPermIfNotExists("FEATURE:READ",   "READ",   "Xem tính năng",   featureFeature);
        Permission featCreate  = createPermIfNotExists("FEATURE:CREATE", "CREATE", "Tạo tính năng",   featureFeature);
        Permission featUpdate  = createPermIfNotExists("FEATURE:UPDATE", "UPDATE", "Sửa tính năng",   featureFeature);
        Permission featDelete  = createPermIfNotExists("FEATURE:DELETE", "DELETE", "Xóa tính năng",   featureFeature);

        Permission permRead    = createPermIfNotExists("PERMISSION:READ",   "READ",   "Xem phân quyền",  permFeature);
        Permission permCreate  = createPermIfNotExists("PERMISSION:CREATE", "CREATE", "Tạo phân quyền",  permFeature);
        Permission permUpdate  = createPermIfNotExists("PERMISSION:UPDATE", "UPDATE", "Sửa phân quyền",  permFeature);
        Permission permDelete  = createPermIfNotExists("PERMISSION:DELETE", "DELETE", "Xóa phân quyền",  permFeature);
        Permission permAssign  = createPermIfNotExists("PERMISSION:ASSIGN", "ASSIGN", "Gán phân quyền",  permFeature);

        Permission qRead       = createPermIfNotExists("QUESTION:READ",   "READ",   "Xem câu hỏi",     questionFeature);
        Permission qCreate     = createPermIfNotExists("QUESTION:CREATE", "CREATE", "Tạo câu hỏi",     questionFeature);
        Permission qUpdate     = createPermIfNotExists("QUESTION:UPDATE", "UPDATE", "Sửa câu hỏi",     questionFeature);
        Permission qDelete     = createPermIfNotExists("QUESTION:DELETE", "DELETE", "Xóa câu hỏi",     questionFeature);

        // 3. Tạo Roles
        Set<Permission> adminPerms = new HashSet<>(Arrays.asList(
            userRead, userCreate, userUpdate, userDelete,
            roleRead, roleCreate, roleUpdate, roleDelete,
            examRead, examCreate, examUpdate, examDelete,
            testRead, testCreate, testUpdate, testDelete,
            featRead, featCreate, featUpdate, featDelete,
            permRead, permCreate, permUpdate, permDelete, permAssign,
            qRead, qCreate, qUpdate, qDelete
        ));
        Role adminRole = createRoleIfNotExists("ADMIN", "Quản trị viên", adminPerms);

        Set<Permission> teacherPerms = new HashSet<>(Arrays.asList(
            userRead,
            examRead, examCreate, examUpdate,
            testRead, testCreate, testUpdate,
            qRead, qCreate, qUpdate, qDelete
        ));
        Role teacherRole = createRoleIfNotExists("TEACHER", "Giáo viên", teacherPerms);

        Set<Permission> studentPerms = new HashSet<>(Arrays.asList(
            examRead,
            testRead
        ));
        Role studentRole = createRoleIfNotExists("STUDENT", "Học sinh", studentPerms);

        // 4. Tạo Users
        User admin = createUserIfNotExists("admin", "Admin@123", "admin@hrm.com", "Super Admin", adminRole);
        User teacher = createUserIfNotExists("teacher", "Password@123", "teacher@hrm.com", "Thầy Giáo Nguyễn Văn A", teacherRole);
        User student = createUserIfNotExists("student", "Password@123", "student@hrm.com", "Học Sinh Trần Thị B", studentRole);

        // 5. Build seed data for Exam, Tests, Questions
        seedExamsTestsQuestions(admin, student);

        log.info("=== DataInitializer: Done ===");
    }

    private void seedExamsTestsQuestions(User admin, User student) {
        if (examRepository.count() == 0) {
            log.info("=== Seeding Exams, Tests, Questions ===");
            // Kỳ thi Toán
            Exam examToan = Exam.builder()
                    .name("Kỳ thi Toán Giữa Kỳ")
                    .description("Kiểm tra kiến thức Toán học giữa học kỳ 1")
                    .startTime(LocalDateTime.now().minusDays(1))
                    .endTime(LocalDateTime.now().plusDays(7))
                    .createdBy(admin)
                    .status("ACTIVE")
                    .createdAt(LocalTime.now())
                    .students(new HashSet<>(Collections.singletonList(student)))
                    .build();
            examToan = examRepository.save(examToan);

            // Kỳ thi Tiếng Anh
            Exam examVan = Exam.builder()
                    .name("Kỳ thi Tiếng Anh Cuối Kỳ")
                    .description("Đánh giá năng lực tiếng Anh cuối năm")
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now().plusDays(10))
                    .createdBy(admin)
                    .status("ACTIVE")
                    .createdAt(LocalTime.now())
                    .students(new HashSet<>(Collections.singletonList(student)))
                    .build();
            examVan = examRepository.save(examVan);

            log.info("  [+] Seeded 2 exams");

            // Câu hỏi Toán
            Question qToan1 = createMultipleChoiceQuestion(
                    "Theo bạn, 1 + 1 bằng mấy?", "MULTIPLE_CHOICE", "EASY", admin,
                    "0", false, "1", false, "2", true, "3", false);

            Question qToan2 = createMultipleChoiceQuestion(
                    "Căn bậc hai của 16 là bao nhiêu?", "MULTIPLE_CHOICE", "NORMAL", admin,
                    "2", false, "4", true, "8", false, "16", false);

            Question qToan3 = createMultipleChoiceQuestion(
                    "Đạo hàm của x^2 là gì?", "MULTIPLE_CHOICE", "HARD", admin,
                    "x", false, "2x", true, "x^2", false, "2", false);

            // Câu hỏi Anh
            Question qAnh1 = createMultipleChoiceQuestion(
                    "What is the capital of Vietnam?", "MULTIPLE_CHOICE", "EASY", admin,
                    "Ho Chi Minh City", false, "Hanoi", true, "Da Nang", false, "Hue", false);

            Question qAnh2 = createMultipleChoiceQuestion(
                    "She _____ to the store every day.", "MULTIPLE_CHOICE", "NORMAL", admin,
                    "go", false, "goes", true, "going", false, "gone", false);

            log.info("  [+] Seeded 5 questions");

            // Đề thi Toán
            Test testToanA = Test.builder()
                    .title("Đề thi Toán học - Mã 101")
                    .exam(examToan)
                    .durationMinutes(45)
                    .totalScore(100)
                    .createdBy(admin)
                    .createAt(LocalDateTime.now())
                    .questions(new HashSet<>(Arrays.asList(qToan1, qToan2, qToan3)))
                    .build();
            testRepository.save(testToanA);

            // Đề thi Tiếng Anh
            Test testAnhA = Test.builder()
                    .title("Đề thi Tiếng Anh - Mã 201")
                    .exam(examVan)
                    .durationMinutes(60)
                    .totalScore(100)
                    .createdBy(admin)
                    .createAt(LocalDateTime.now())
                    .questions(new HashSet<>(Arrays.asList(qAnh1, qAnh2)))
                    .build();
            testRepository.save(testAnhA);

            log.info("  [+] Seeded 2 tests");

            // Seed ExamAttempt for testToanA by student
            ExamAttempt attemptToan = ExamAttempt.builder()
                    .user(student)
                    .test(testToanA)
                    .startTime(LocalDateTime.now().minusMinutes(50))
                    .submitTime(LocalDateTime.now().minusMinutes(5)) // took 45 mins
                    .score(66.67) // 2/3 correct
                    .totalCorrect(2)
                    .studentAnswers(new HashSet<>())
                    .build();

            // First query has 1 question qToan1
            Answer correctToan1 = qToan1.getAnswers().stream().filter(Answer::getIsCorrect).findFirst().orElse(null);
            attemptToan.addStudentAnswer(StudentAnswer.builder()
                    .question(qToan1)
                    .selectedAnswer(correctToan1)
                    .isCorrect(true)
                    .build());

            // qToan2 answered wrong
            Answer wrongToan2 = qToan2.getAnswers().stream().filter(a -> !a.getIsCorrect()).findFirst().orElse(null);
            attemptToan.addStudentAnswer(StudentAnswer.builder()
                    .question(qToan2)
                    .selectedAnswer(wrongToan2)
                    .isCorrect(false)
                    .build());

            // qToan3 answered correctly
            Answer correctToan3 = qToan3.getAnswers().stream().filter(Answer::getIsCorrect).findFirst().orElse(null);
            attemptToan.addStudentAnswer(StudentAnswer.builder()
                    .question(qToan3)
                    .selectedAnswer(correctToan3)
                    .isCorrect(true)
                    .build());

            examAttemptRepository.save(attemptToan);
            log.info("  [+] Seeded 1 ExamAttempt for Math test");
        }
    }

    private Question createMultipleChoiceQuestion(String content, String type, String difficulty, User createdBy,
                                                  String a1, boolean c1,
                                                  String a2, boolean c2,
                                                  String a3, boolean c3,
                                                  String a4, boolean c4) {
        Question q = Question.builder()
                .content(content)
                .questionType(type)
                .difficulty(difficulty)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .answers(new HashSet<>())
                .build();

        q.addAnswer(Answer.builder().content(a1).isCorrect(c1).build());
        q.addAnswer(Answer.builder().content(a2).isCorrect(c2).build());
        q.addAnswer(Answer.builder().content(a3).isCorrect(c3).build());
        q.addAnswer(Answer.builder().content(a4).isCorrect(c4).build());

        return questionRepository.save(q);
    }

    private Feature createFeatureIfNotExists(String code, String name) {
        return featureRepository.findByCode(code).orElseGet(() -> {
            Feature f = Feature.builder()
                    .code(code)
                    .name(name)
                    .description("Tính năng " + name)
                    .build();
            log.info("  [+] Feature: {}", code);
            return featureRepository.save(f);
        });
    }

    private Permission createPermIfNotExists(String code, String action, String name, Feature feature) {
        return permissionRepository.findByCode(code).orElseGet(() -> {
            Permission p = Permission.builder()
                    .code(code)
                    .action(action)
                    .name(name)
                    .description("Quyền " + name)
                    .feature(feature)
                    .build();
            log.info("  [+] Permission: {}", code);
            return permissionRepository.save(p);
        });
    }

    private Role createRoleIfNotExists(String code, String name, Set<Permission> permissions) {
        return roleRepository.findByCode(code).orElseGet(() -> {
            Role r = Role.builder()
                    .code(code)
                    .name(name)
                    .description("Vai trò " + name)
                    .permissions(permissions)
                    .build();
            log.info("  [+] Role: {} ({} permissions)", code, permissions.size());
            return roleRepository.save(r);
        });
    }

    private User createUserIfNotExists(String username, String password, String email, String fullName, Role role) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            return optionalUser.get();
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .fullName(fullName)
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singleton(role)))
                .build();
        userRepository.save(user);
        log.info("  [+] User created: {} / {}", username, password);
        return user;
    }
}
