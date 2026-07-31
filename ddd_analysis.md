# Phân Tích & Kế Hoạch Refactor: Modular Monolith với DDD
### Dự án: HeThongThiTracNghiem (Hệ thống Thi Trắc Nghiệm Online)

---

## PHẦN 1 — PHÂN TÍCH HỆ THỐNG HIỆN TẠI

### 1.1 Kiến trúc hiện tại

Dự án đang theo mô hình **Layered Architecture (Technical Layer)**:

```
com.hrm.project_spring/
├── controller/        ← Tất cả controller ném vào một chỗ
├── service/           ← Tất cả service ném vào một chỗ
│   └── user/          ← (ngoại lệ duy nhất được tách)
├── repository/        ← Tất cả repository ném vào một chỗ
├── entity/            ← Tất cả entity ném vào một chỗ
├── dto/               ← Tất cả DTO ném vào một chỗ
├── mapper/            ← Tất cả mapper ném vào một chỗ
├── enums/             ← Tất cả enum ném vào một chỗ
├── exception/
├── config/
├── security/
└── scheduler/
```

**Stack công nghệ:**
- Spring Boot 3.2.3, Java 17
- Spring Data JPA + PostgreSQL
- Spring Security + JWT (jjwt 0.11.5)
- Apache POI (Excel), OpenCSV, Jsoup, Spring Mail
- Lombok, Swagger/OpenAPI

---

### 1.2 Các Business Domain phát hiện được

| # | Domain | Mô tả |
|---|--------|-------|
| 1 | **Identity & Access (IAM)** | User, Role, Permission, Feature — quản lý danh tính và phân quyền |
| 2 | **Catalog (Danh mục học thuật)** | Subject, Chapter, Tag — danh mục môn học, chương, thẻ |
| 3 | **Question Bank (Ngân hàng câu hỏi)** | Question, QuestionOption — soạn/quản lý câu hỏi |
| 4 | **Exam Management (Quản lý kỳ thi)** | Exam, Test — tạo kỳ thi, đề thi |
| 5 | **Classroom (Lớp học)** | ClassRoom — quản lý lớp, gán học sinh |
| 6 | **Examination (Thi cử / Làm bài)** | ExamAttempt, StudentAnswer — luồng học sinh làm bài |
| 7 | **Notification (Thông báo)** | EmailService — gửi email kích hoạt, reset pass |
| 8 | **Audit (Kiểm toán)** | AuditLog — ghi nhật ký hành động |
| 9 | **Dashboard (Báo cáo)** | DashboardService — tổng hợp số liệu |

---

### 1.3 Bounded Context Map

```
┌──────────────────────────────────────────────────────────────┐
│                    BOUNDED CONTEXT MAP                        │
│                                                               │
│  ┌──────────────┐     ┌─────────────────┐                    │
│  │  IAM Context │────▶│ Catalog Context  │                    │
│  │  (User/Role) │     │ (Subject/Chapter)│                    │
│  └──────┬───────┘     └────────┬────────┘                    │
│         │                      │                              │
│         ▼                      ▼                              │
│  ┌──────────────┐     ┌─────────────────┐                    │
│  │  Classroom   │     │  Question Bank  │                    │
│  │  Context     │     │  Context        │                    │
│  └──────┬───────┘     └────────┬────────┘                    │
│         │                      │                              │
│         └──────────┬───────────┘                              │
│                    ▼                                          │
│          ┌──────────────────┐                                 │
│          │  Exam Management │                                  │
│          │  Context         │                                  │
│          └────────┬─────────┘                                 │
│                   │                                           │
│                   ▼                                           │
│          ┌──────────────────┐   ┌──────────────┐             │
│          │  Examination     │──▶│  Audit       │             │
│          │  (Taking exam)   │   │  Context     │             │
│          └────────┬─────────┘   └──────────────┘             │
│                   │                                           │
│                   ▼                                           │
│          ┌──────────────────┐   ┌──────────────┐             │
│          │  Dashboard       │   │ Notification  │             │
│          │  Context         │   │  Context      │             │
│          └──────────────────┘   └──────────────┘             │
└──────────────────────────────────────────────────────────────┘
```

---

### 1.4 Entity chính và vai trò

| Entity | Vai trò hiện tại | Bounded Context |
|--------|-----------------|-----------------|
| `User` | Aggregate Root: học sinh, giáo viên, admin | IAM |
| `Role` | Entity: gắn với User | IAM |
| `Permission` | Entity: gắn với Role | IAM |
| `Feature` | Entity: nhóm Permission theo feature | IAM |
| `RefreshToken` | Entity: quản lý phiên JWT | IAM |
| `PasswordHistory` | Entity: lịch sử mật khẩu | IAM |
| `TokenBlacklist` | Entity: JWT bị thu hồi | IAM |
| `Subject` | Aggregate Root: môn học | Catalog |
| `Chapter` | Entity thuộc Subject | Catalog |
| `Tag` | Entity độc lập: tag câu hỏi | Catalog |
| `Question` | Aggregate Root: câu hỏi | Question Bank |
| `QuestionOption` | Entity thuộc Question | Question Bank |
| `Exam` | Aggregate Root: kỳ thi | Exam Management |
| `Test` | Entity thuộc Exam: đề thi | Exam Management |
| `ClassRoom` | Aggregate Root: lớp học | Classroom |
| `ExamAttempt` | Aggregate Root: lần thi | Examination |
| `StudentAnswer` | Entity thuộc ExamAttempt | Examination |
| `AuditLog` | Aggregate Root: log hành động | Audit |

---

### 1.5 Use Cases chính

**IAM:**
- UC-01: Đăng nhập (login, refresh token, logout)
- UC-02: Đổi mật khẩu, reset mật khẩu qua email
- UC-03: Kích hoạt tài khoản qua email
- UC-04: Tạo/sửa/xóa/khoá User
- UC-05: Gán/gỡ Role cho User
- UC-06: CRUD Role và Permission

**Catalog:**
- UC-07: CRUD Subject (môn học)
- UC-08: CRUD Chapter (chương theo Subject)

**Question Bank:**
- UC-09: CRUD câu hỏi (đơn/bộ/import Excel)
- UC-10: Gán Tag cho câu hỏi
- UC-11: Filter câu hỏi theo Subject, Chapter, Tag, bloomLevel

**Exam Management:**
- UC-12: Tạo/sửa/xoá kỳ thi (Exam)
- UC-13: Tạo đề thi (Test) thuộc kỳ thi
- UC-14: Gán câu hỏi vào đề thi
- UC-15: Gán học sinh vào kỳ thi (từng người hoặc từ lớp)

**Classroom:**
- UC-16: CRUD lớp học
- UC-17: Thêm/xóa học sinh khỏi lớp
- UC-18: Gán lớp vào kỳ thi

**Examination:**
- UC-19: Học sinh bắt đầu làm bài (start attempt)
- UC-20: Học sinh nộp bài (submit attempt)
- UC-21: Xem kết quả bài thi

**Dashboard:**
- UC-22: Xem dashboard Admin (thống kê tổng)
- UC-23: Xem dashboard Học sinh (kỳ thi của tôi)

---

### 1.6 Vi phạm DDD trong code hiện tại

| # | Vi phạm | Vị trí | Mức độ |
|---|---------|--------|--------|
| 1 | **Anemic Domain Model** — Entity chỉ có getter/setter, không có behavior | Tất cả entity | 🔴 Nghiêm trọng |
| 2 | **Fat Service** — `UserService.java` (834 lines) làm tất cả mọi thứ | `UserService` | 🔴 Nghiêm trọng |
| 3 | **Cross-context Entity reference** — `ExamService` trực tiếp dùng `User`, `ClassRoom` entity từ context khác | `ExamService` | 🔴 Nghiêm trọng |
| 4 | **Technical layering** — Không có boundary giữa các domain | Toàn bộ package | 🔴 Nghiêm trọng |
| 5 | **Service gọi Repository của context khác** — `ClassRoomService` gọi `ExamRepository` | `ClassRoomService` | 🟠 Quan trọng |
| 6 | **Business logic trong Entity** — Không có, mọi logic đều ở Service | Toàn bộ service | 🟠 Quan trọng |
| 7 | **Mapper lẫn lộn** — `Subject.java` import `SubjectResponse` (DTO trong Entity!) | `Subject.java`, `Chapter.java` | 🟠 Quan trọng |
| 8 | **No Domain Events** — Các tác vụ cross-domain bị gọi trực tiếp (gửi email, audit) | `UserService`, `AuthService` | 🟡 Cần cải thiện |
| 9 | **SecurityContext trong Service** — Service tầng domain bị ô nhiễm bởi Spring Security | `ExamService`, `TestService`, nhiều service | 🟡 Cần cải thiện |
| 10 | **Enum không có domain semantics** — `Exam.status` là `String` thay vì enum | `Exam.java` | 🟡 Cần cải thiện |
| 11 | **Mapper không tách biệt** — `TestService.mapToResponse()` làm mapping inline | `TestService` | 🟡 Cần cải thiện |

---

## PHẦN 2 — ĐỀ XUẤT CHIA MODULE

### 2.1 Cấu trúc package đề xuất

```
com.hrm.project_spring/
│
├── _shared/                          ← Shared Kernel (dùng chung toàn bộ app)
│   ├── common/
│   │   ├── PageResponse.java
│   │   └── AuditableEntity.java      ← Base entity với createdAt, updatedAt
│   ├── exception/
│   │   ├── DomainException.java
│   │   ├── ResourceNotFoundException.java
│   │   └── BusinessRuleViolationException.java
│   └── event/
│       └── DomainEvent.java          ← Base event interface
│
├── iam/                              ← Identity & Access Management
│   ├── domain/
│   │   ├── model/
│   │   │   ├── User.java             ← Aggregate Root
│   │   │   ├── Role.java
│   │   │   ├── Permission.java
│   │   │   ├── Feature.java
│   │   │   ├── RefreshToken.java
│   │   │   └── PasswordHistory.java
│   │   ├── valueobject/
│   │   │   ├── Email.java
│   │   │   ├── Username.java
│   │   │   └── UserStatus.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java   ← Interface (port)
│   │   │   └── RoleRepository.java
│   │   ├── service/
│   │   │   └── PasswordPolicyService.java  ← Domain Service
│   │   └── event/
│   │       ├── UserCreatedEvent.java
│   │       └── UserLockedEvent.java
│   ├── application/
│   │   ├── AuthApplicationService.java
│   │   ├── UserApplicationService.java
│   │   ├── RoleApplicationService.java
│   │   └── PermissionApplicationService.java
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── UserJpaRepository.java
│   │   │   └── RoleJpaRepository.java
│   │   └── security/
│   │       ├── JwtService.java
│   │       ├── CustomUserDetails.java
│   │       └── CustomUserDetailsService.java
│   └── presentation/
│       ├── AuthController.java
│       ├── UserController.java
│       ├── RoleController.java
│       └── PermissionController.java
│
├── catalog/                          ← Subject & Chapter Catalog
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Subject.java          ← Aggregate Root
│   │   │   ├── Chapter.java
│   │   │   └── Tag.java
│   │   └── repository/
│   │       ├── SubjectRepository.java
│   │       └── ChapterRepository.java
│   ├── application/
│   │   ├── SubjectApplicationService.java
│   │   └── ChapterApplicationService.java
│   ├── infrastructure/
│   │   └── persistence/
│   │       ├── SubjectJpaRepository.java
│   │       └── ChapterJpaRepository.java
│   └── presentation/
│       ├── SubjectController.java
│       └── ChapterController.java
│
├── question/                         ← Question Bank
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Question.java         ← Aggregate Root
│   │   │   └── QuestionOption.java   ← Entity (child)
│   │   ├── valueobject/
│   │   │   ├── QuestionType.java
│   │   │   ├── QuestionStatus.java
│   │   │   └── BloomLevel.java
│   │   ├── repository/
│   │   │   └── QuestionRepository.java
│   │   └── service/
│   │       └── QuestionValidationService.java
│   ├── application/
│   │   ├── QuestionApplicationService.java
│   │   └── QuestionImportService.java
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   └── QuestionJpaRepository.java
│   │   └── importer/
│   │       └── ExcelQuestionImporter.java
│   └── presentation/
│       └── QuestionController.java
│
├── exam/                             ← Exam & Test Management
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Exam.java             ← Aggregate Root
│   │   │   └── Test.java             ← Entity (child)
│   │   ├── valueobject/
│   │   │   └── ExamStatus.java
│   │   ├── repository/
│   │   │   ├── ExamRepository.java
│   │   │   └── TestRepository.java
│   │   └── service/
│   │       └── ExamScheduleService.java
│   ├── application/
│   │   ├── ExamApplicationService.java
│   │   └── TestApplicationService.java
│   ├── infrastructure/
│   │   └── persistence/
│   │       ├── ExamJpaRepository.java
│   │       └── TestJpaRepository.java
│   └── presentation/
│       ├── ExamController.java
│       └── TestController.java
│
├── classroom/                        ← Classroom Management
│   ├── domain/
│   │   ├── model/
│   │   │   └── ClassRoom.java        ← Aggregate Root
│   │   ├── valueobject/
│   │   │   └── ClassStatus.java
│   │   └── repository/
│   │       └── ClassRoomRepository.java
│   ├── application/
│   │   └── ClassRoomApplicationService.java
│   ├── infrastructure/
│   │   └── persistence/
│   │       └── ClassRoomJpaRepository.java
│   └── presentation/
│       └── ClassRoomController.java
│
├── examination/                      ← Taking Exam (Làm bài thi)
│   ├── domain/
│   │   ├── model/
│   │   │   ├── ExamAttempt.java      ← Aggregate Root
│   │   │   └── StudentAnswer.java    ← Entity (child)
│   │   ├── repository/
│   │   │   └── ExamAttemptRepository.java
│   │   └── service/
│   │       └── ScoreCalculationService.java
│   ├── application/
│   │   └── ExaminationApplicationService.java
│   ├── infrastructure/
│   │   └── persistence/
│   │       └── ExamAttemptJpaRepository.java
│   └── presentation/
│       └── ExaminationController.java
│
├── notification/                     ← Notification (Email)
│   ├── domain/
│   │   └── service/
│   │       └── NotificationService.java  ← Interface (port)
│   ├── application/
│   │   └── EmailNotificationService.java
│   └── infrastructure/
│       └── email/
│           └── SmtpEmailSender.java
│
├── audit/                            ← Audit Logging
│   ├── domain/
│   │   ├── model/
│   │   │   └── AuditLog.java
│   │   └── repository/
│   │       └── AuditLogRepository.java
│   ├── application/
│   │   └── AuditApplicationService.java
│   └── infrastructure/
│       └── persistence/
│           └── AuditLogJpaRepository.java
│
└── dashboard/                        ← Dashboard & Reporting
    ├── application/
    │   └── DashboardApplicationService.java
    └── presentation/
        └── DashboardController.java
```

### 2.2 Lý do chia như trên

| Module | Lý do |
|--------|-------|
| `iam` | User + Role + Permission + Auth là một bounded context nhất quán về Identity. Thay đổi cách phân quyền không nên ảnh hưởng đến `exam` hay `question`. |
| `catalog` | Subject + Chapter là *master data* dùng chung, nhưng logic nghiệp vụ của nó không nằm trong exam hay question. Tách ra để dễ quản lý. |
| `question` | Ngân hàng câu hỏi có domain logic riêng: Bloom level, import Excel, validate options. Tách để có thể phát triển độc lập. |
| `exam` | Exam + Test là quản lý *cấu trúc* kỳ thi. Tách khỏi *luồng thi* (examination). |
| `classroom` | Lớp học là context quản lý nhóm học sinh, không phụ thuộc trực tiếp vào exam. |
| `examination` | Luồng *làm bài thi* là core business riêng biệt, có logic chấm điểm phức tạp. |
| `notification` | Email/SMS là cross-cutting concern, cần tách thành port-adapter để dễ thay thế. |
| `audit` | Ghi log là cross-cutting concern thuần túy. |
| `dashboard` | Aggregates data từ nhiều domain, không có business logic riêng. |
| `_shared` | Chứa shared kernel: base classes, common exceptions, domain events. |

---

## PHẦN 3 — THIẾT KẾ DDD CHO TỪNG MODULE

### 3.1 Module `iam`

```
Aggregate Root:  User
  ├── Entity:    Role (id-referenced)
  │   └── Entity: Permission
  │               └── Entity: Feature
  ├── Entity:    RefreshToken
  ├── Entity:    PasswordHistory
  └── Entity:    TokenBlacklist

Value Objects:
  ├── Email         — validate format, lowercase normalization
  ├── Username      — validate pattern, lowercase
  ├── UserStatus    — ACTIVE, INACTIVE, LOCKED, DELETED, PENDING
  └── Gender        — MALE, FEMALE, OTHER

Domain Services:
  └── PasswordPolicyService
        ├── validateNewPassword(newPass, history) — kiểm tra policy
        └── isPasswordExpired(changedAt) — kiểm tra hạn mật khẩu

Repository Interfaces (Ports):
  ├── UserRepository      — findByUsername, findByEmail, findByUsernameOrEmail
  ├── RoleRepository      — findByCode, countUsersByRole
  └── PermissionRepository — findByCode

Application Services:
  ├── AuthApplicationService  — login, logout, refreshToken, resetPassword, activate
  ├── UserApplicationService  — CRUD user, import/export, lock/unlock, changePassword
  ├── RoleApplicationService  — CRUD role, assignPermissions
  └── PermissionApplicationService — CRUD permission

Infrastructure:
  ├── UserJpaRepository       (implements UserRepository)
  ├── JwtService              (JWT generation, validation)
  ├── CustomUserDetails       (Spring Security integration)
  └── CustomUserDetailsService

Domain Events:
  ├── UserCreatedEvent    — trigger: gửi email kích hoạt
  ├── UserLockedEvent     — trigger: audit log
  └── PasswordChangedEvent — trigger: lưu history

DTOs (trong application/dto):
  ├── LoginRequest / AuthResponse
  ├── CreateUserRequest / UserResponse
  ├── ChangePasswordRequest / ResetPasswordRequest
  └── RoleRequest / RoleResponse / PermissionResponse
```

---

### 3.2 Module `catalog`

```
Aggregate Root:  Subject
  └── Entity:    Chapter (ordered list)

Aggregate Root:  Tag  (độc lập, dùng cross-module)

Value Objects:   (none đặc biệt)

Repository Interfaces:
  ├── SubjectRepository  — findByCode, existsByCode
  └── ChapterRepository  — findBySubjectId, findByCode

Application Services:
  ├── SubjectApplicationService  — CRUD subject
  └── ChapterApplicationService  — CRUD chapter trong subject

Infrastructure:
  ├── SubjectJpaRepository
  └── ChapterJpaRepository

DTOs:
  ├── SubjectRequest / SubjectResponse
  └── ChapterRequest / ChapterResponse
```

---

### 3.3 Module `question`

```
Aggregate Root:  Question
  └── Entity:    QuestionOption (ordered, has isCorrect flag)
  └── Reference: Tag (Value by ID — cross-context)

Value Objects:
  ├── QuestionType     — SINGLE_CHOICE, MULTIPLE_CHOICE, ESSAY
  ├── QuestionStatus   — DRAFT, ACTIVE, INACTIVE
  └── BloomLevel       — wrapped Integer (1-6)

Domain Services:
  └── QuestionValidationService
        ├── validateOptions(type, options) — SINGLE must have exactly 1 correct
        └── validateScore(score) — BigDecimal > 0

Repository Interfaces:
  ├── QuestionRepository  — findBySubjectId, findByChapterId, findByTags
  └── TagRepository       — findByNameIn, findOrCreate

Application Services:
  ├── QuestionApplicationService  — CRUD question, filter, search
  └── QuestionImportService       — import từ Excel/CSV

Infrastructure:
  ├── QuestionJpaRepository
  └── ExcelQuestionImporter (sử dụng Apache POI)

Domain Events:
  └── QuestionStatusChangedEvent  — khi approve/reject

DTOs:
  ├── CreateQuestionRequest / QuestionResponse
  ├── UpdateQuestionRequest / QuestionDetailResponse
  └── QuestionFilterRequest (subject, chapter, tag, bloom, status)
```

---

### 3.4 Module `exam`

```
Aggregate Root:  Exam
  └── Entity:    Test  (một Exam có nhiều Test/đề)
  └── Reference: Student user IDs (Set<Long> — không dùng entity trực tiếp)

Value Objects:
  └── ExamStatus  — OPEN, CLOSED, UPCOMING (thay cho String hiện tại)

Domain Services:
  └── ExamScheduleService
        └── isExamOpen(startTime, endTime) — kiểm tra thời gian thi

Repository Interfaces:
  ├── ExamRepository  — findByStudentId, countByStatus, findByIdWithStudents
  └── TestRepository  — findByExamId

Application Services:
  ├── ExamApplicationService  — CRUD exam, assignStudents, assignClass
  └── TestApplicationService  — CRUD test, assignQuestions

Infrastructure:
  ├── ExamJpaRepository
  └── TestJpaRepository

DTOs:
  ├── ExamRequest / ExamDetailResponse / ExamListResponse
  ├── TestRequest / TestResponse
  └── AssignQuestionsRequest / AssignStudentsRequest
```

---

### 3.5 Module `classroom`

```
Aggregate Root:  ClassRoom
  └── Reference: teacherId (Long — ID of User)
  └── Reference: studentIds (Set<Long> — IDs of Users)

Value Objects:
  └── ClassStatus  — ACTIVE, INACTIVE, ARCHIVED

Repository Interfaces:
  └── ClassRoomRepository  — findByCode, existsByCode

Application Services:
  └── ClassRoomApplicationService
        — CRUD classroom, addStudents, removeStudents, getStudentIds

Infrastructure:
  └── ClassRoomJpaRepository

DTOs:
  ├── ClassRoomRequest / ClassRoomResponse
  └── ClassSummaryResponse
```

---

### 3.6 Module `examination` (Làm bài thi)

```
Aggregate Root:  ExamAttempt
  └── Entity:    StudentAnswer  (answer per question)
  └── Reference: userId (Long)
  └── Reference: testId (Long)

Value Objects:
  └── AttemptStatus  — IN_PROGRESS, SUBMITTED, EXPIRED

Domain Services:
  └── ScoreCalculationService
        ├── calculate(attempt, answers) — tính điểm tự động
        └── isTimeExpired(startTime, durationMinutes) — kiểm tra hết giờ

Repository Interfaces:
  └── ExamAttemptRepository
        — findByUserIdAndTestId, countByUserId, findAverageScoreByUserId

Application Services:
  └── ExaminationApplicationService
        — startAttempt, submitAttempt, getResult, getMyAttempts

Infrastructure:
  └── ExamAttemptJpaRepository

Domain Events:
  └── AttemptSubmittedEvent  — trigger: cập nhật dashboard, gửi thông báo

DTOs:
  ├── StartAttemptRequest / AttemptResponse
  ├── SubmitAttemptRequest (with answers)
  └── AttemptResultResponse
```

---

### 3.7 Module `notification`

```
Domain Service (Port/Interface):
  └── NotificationService
        ├── sendActivationEmail(email, token)
        ├── sendResetPasswordEmail(email, token)
        └── sendPasswordChangedNotification(email)

Infrastructure (Adapter):
  └── SmtpEmailSender  (implements NotificationService)
        — dùng Spring Mail, Thymeleaf template

(Không có Aggregate Root — đây là pure infrastructure concern)
```

---

### 3.8 Module `audit`

```
Aggregate Root:  AuditLog  (immutable — chỉ create, không update/delete)

Repository Interfaces:
  └── AuditLogRepository  — findByUserId, findByAction, findByDateRange

Application Services:
  └── AuditApplicationService
        — log(userId, action, details), search, export

Infrastructure:
  └── AuditLogJpaRepository

DTOs:
  └── AuditLogResponse / AuditSearchRequest
```

---

### 3.9 Module `dashboard`

```
(Không có Aggregate Root — đây là query/reporting concern)

Application Services:
  └── DashboardApplicationService
        — getAdminDashboard(), getStudentDashboard(userId), getMyExams(userId)
        — Gọi qua các Repository Interface của các domain khác (read-only)

DTOs:
  ├── AdminDashboardResponse
  ├── StudentDashboardResponse
  └── MyExamResponse
```

---

## PHẦN 4 — THIẾT KẾ DEPENDENCY

### 4.1 Quy tắc phụ thuộc

```
RULE 1: domain layer không biết gì về Spring, JPA, HTTP
RULE 2: application layer chỉ dùng domain interfaces (ports)
RULE 3: infrastructure layer implements domain interfaces
RULE 4: presentation layer chỉ gọi application services
RULE 5: modules chỉ giao tiếp qua application service interface,
        KHÔNG gọi trực tiếp infrastructure của module khác
```

### 4.2 Dependency Tree

```
                         ┌─────────────┐
                         │   _shared   │  (shared kernel)
                         └──────┬──────┘
                                │ (tất cả module đều dùng)
          ┌─────────────────────┼──────────────────────┐
          │                     │                      │
    ┌─────▼──────┐       ┌──────▼──────┐       ┌──────▼──────┐
    │    iam     │       │   catalog   │       │    audit    │
    │  (User/    │       │  (Subject/  │       │  (AuditLog) │
    │  Role/Auth)│       │   Chapter)  │       └─────────────┘
    └─────┬──────┘       └──────┬──────┘
          │                     │
          │              ┌──────▼──────┐
          │              │  question   │
          │              │  (Question/ │
          │              │  Option)    │
          │              └──────┬──────┘
          │                     │
    ┌─────▼──────┐       ┌──────▼──────┐
    │  classroom │       │    exam     │
    │ (ClassRoom)│       │ (Exam/Test) │
    └─────┬──────┘       └──────┬──────┘
          │                     │
          └──────────┬──────────┘
                     │
              ┌──────▼──────┐
              │ examination │
              │(ExamAttempt)│
              └──────┬──────┘
                     │
          ┌──────────┴──────────┐
          │                     │
   ┌──────▼──────┐    ┌─────────▼──────┐
   │  dashboard  │    │  notification  │
   │ (reporting) │    │  (email/notif) │
   └─────────────┘    └────────────────┘
```

### 4.3 Bảng phụ thuộc chi tiết

| Module | Được phép gọi | Giao tiếp qua | KHÔNG được gọi |
|--------|--------------|---------------|----------------|
| `_shared` | - | - | Bất kỳ domain module nào |
| `iam` | `_shared`, `notification` | Interface `NotificationService` | `exam`, `question`, `classroom` |
| `catalog` | `_shared` | - | `iam`, `exam`, `question` |
| `question` | `_shared`, `catalog` (read-only) | `SubjectQuery`, `ChapterQuery` interface | `iam`, `exam`, `classroom` |
| `exam` | `_shared`, `catalog` (read-only) | ID references only | `iam` trực tiếp, `classroom` trực tiếp |
| `classroom` | `_shared` | - | `exam`, `question` |
| `examination` | `_shared`, `exam` (read), `question` (read) | ID references, Query interfaces | Gọi trực tiếp `iam` domain |
| `notification` | `_shared` | - | Bất kỳ domain module nào |
| `audit` | `_shared` | - | Bất kỳ domain module nào |
| `dashboard` | `_shared`, read từ tất cả module | Repository interfaces (read-only queries) | Không được thay đổi data |

### 4.4 Cross-module Communication Pattern

**Pattern được dùng:** Anti-Corruption Layer + ID References

```java
// ĐÚNG — exam module chỉ lưu userId, không giữ User entity
public class Exam {
    private Set<Long> studentIds = new HashSet<>(); // chỉ lưu ID
}

// ĐÚNG — khi cần thông tin User, dùng query interface
public interface UserQueryPort {
    Optional<UserSummary> findById(Long id);
    boolean existsById(Long id);
    boolean hasRole(Long userId, String roleCode);
}

// SAI — exam domain KHÔNG được dùng User entity trực tiếp
// private Set<User> students; ← Vi phạm boundary
```

---

## PHẦN 5 — KẾ HOẠCH REFACTOR TỪNG BƯỚC

### Nguyên tắc

- **KHÔNG break build** sau mỗi bước
- **KHÔNG đổi business logic** — chỉ re-organize
- **KHÔNG đổi API** — controller paths giữ nguyên
- **KHÔNG đổi database** — table names giữ nguyên

---

### PHASE 0 — Chuẩn bị (Không thay đổi code)

**Mục tiêu:** Hiểu rõ toàn bộ dependency hiện tại

- [ ] Vẽ dependency graph hiện tại bằng IntelliJ Diagrams
- [ ] Tạo branch `feature/ddd-refactor` từ main
- [ ] Đảm bảo test suite hiện tại pass (nếu có)

---

### BƯỚC 1 — Tạo `_shared` module và di chuyển shared code

**Phạm vi thay đổi:** Nhỏ, ít rủi ro

**File cần tạo/sửa:**
```
+ _shared/common/PageResponse.java          (move từ dto/common/)
+ _shared/exception/DomainException.java    (new base exception)
+ _shared/exception/ResourceNotFoundException.java  (refactor từ exception/)
+ _shared/event/DomainEvent.java            (new interface)
```

**Lý do:** Tạo nền tảng để các module sau kế thừa. Không ảnh hưởng business logic.

---

### BƯỚC 2 — Refactor `catalog` module (Subject + Chapter)

**Phạm vi thay đổi:** Nhỏ, ít dependency

**File cần di chuyển/tạo:**
```
entity/Subject.java       → catalog/domain/model/Subject.java
entity/Chapter.java       → catalog/domain/model/Chapter.java
entity/Tag.java           → catalog/domain/model/Tag.java
service/SubjectService.java → catalog/application/SubjectApplicationService.java
service/ChapterService.java → catalog/application/ChapterApplicationService.java
repository/SubjectRepository.java → catalog/domain/repository/SubjectRepository.java (interface)
repository/ChapterRepository.java → catalog/domain/repository/ChapterRepository.java (interface)
controller/SubjectController.java → catalog/presentation/SubjectController.java
controller/ChapterController.java → catalog/presentation/ChapterController.java
```

**Fix vi phạm:**
- Xóa import `SubjectResponse` trong `Subject.java`
- Xóa import `ChapterResponse` trong `Chapter.java`

---

### BƯỚC 3 — Refactor `iam` module (User + Role + Auth)

**Phạm vi thay đổi:** Lớn nhất, nhiều dependency nhất

**Thứ tự trong bước này:**
1. Di chuyển entities (User, Role, Permission, Feature, RefreshToken, PasswordHistory, TokenBlacklist)
2. Di chuyển repositories
3. Di chuyển security/ (JwtService, CustomUserDetails)
4. Di chuyển và tách AuthService → AuthApplicationService
5. Di chuyển UserService → UserApplicationService
6. Di chuyển controllers

**Key change:** Tạo `UserQueryPort` interface để các module khác gọi IAM mà không phụ thuộc User entity trực tiếp.

---

### BƯỚC 4 — Refactor `question` module

**Phạm vi thay đổi:** Vừa, phụ thuộc catalog

**File cần di chuyển:**
```
entity/Question.java → question/domain/model/Question.java
entity/QuestionOption.java → question/domain/model/QuestionOption.java
service/QuestionService.java → question/application/QuestionApplicationService.java
service/user/UserImportService.java → question/application/QuestionImportService.java (phần import câu hỏi)
```

---

### BƯỚC 5 — Refactor `exam` module

**Phụ thuộc:** catalog (read-only), iam (qua UserQueryPort)

**Key change:**
- `Exam.students` từ `Set<User>` → `Set<Long>` (studentIds)
- Tạo `ExamStatus` enum thay cho `String`

---

### BƯỚC 6 — Refactor `classroom` module

---

### BƯỚC 7 — Refactor `examination` module

**Phụ thuộc:** exam (read-only), question (read-only)

---

### BƯỚC 8 — Refactor `notification` + `audit` modules

**Pattern:** Port (interface trong domain) + Adapter (implementation trong infrastructure)

---

### BƯỚC 9 — Refactor `dashboard` module

**Phụ thuộc:** Read-only queries từ tất cả modules

---

### BƯỚC 10 — Làm sạch và hoàn thiện

- Xóa package cũ (entity/, service/, repository/, controller/ gốc)
- Thêm Domain Events cho cross-module communication
- Thêm validation annotation thay cho if-throw thủ công
- Tách mapping ra khỏi Service (dùng dedicated Mapper classes)

---

## PHẦN 6 — QUY TẮC BẤT BIẾN

```
✅ ĐƯỢC phép:
   - Re-organize packages
   - Extract interface từ concrete class
   - Tạo Domain Event thay cho direct method call
   - Thêm Value Object wrapper

❌ KHÔNG được phép:
   - Đổi tên bảng database
   - Đổi API endpoint path
   - Thay đổi logic nghiệp vụ
   - Thêm framework mới (chỉ dùng những gì đã có)
   - Refactor nhiều module cùng một lúc
```

---

> **Lưu ý:** Tài liệu này là bản phân tích và kế hoạch. Execution sẽ được thực hiện từng bước, mỗi bước đều phải đảm bảo project build được.
