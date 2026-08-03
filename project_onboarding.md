# 📘 HCM Talent Management — Onboarding Guide cho Java Backend Intern

> **Mentor note**: Đây là phân tích nhanh nhất để bạn hiểu dự án trong 2 ngày. Đọc theo thứ tự từ trên xuống.

---

## 1. Tổng quan dự án

### Dự án làm gì?
**HCM Talent Management** là một module thuộc hệ thống HCM (Human Capital Management) của **Evotek**.

Nó quản lý **toàn bộ quy trình tuyển dụng**, từ lúc tạo yêu cầu tuyển dụng đến khi ứng viên được onboard vào công ty.

### Business chính
| Nghiệp vụ | Mô tả |
|---|---|
| Recruitment Request | Phòng ban tạo yêu cầu tuyển dụng, gửi duyệt |
| Candidate | Ứng viên nộp CV, được phân công vào yêu cầu tuyển dụng |
| Interview | Lên lịch phỏng vấn nhiều vòng (round) cho ứng viên |
| Evaluation | Đánh giá ứng viên qua từng vòng phỏng vấn |
| Onboard | Ứng viên pass → tạo task onboard |
| Screening Question | Câu hỏi sàng lọc đính kèm yêu cầu tuyển dụng |
| Proposal | Đề xuất ứng viên để phê duyệt |
| AI Matching | Gợi ý ứng viên / công việc phù hợp bằng AI |

### Người dùng của hệ thống
- **HR Manager / Recruiter**: Quản lý yêu cầu tuyển dụng, phỏng vấn
- **Department Manager**: Tạo yêu cầu tuyển dụng, duyệt
- **Interviewer**: Thực hiện đánh giá phỏng vấn
- **Applicant (ứng viên ngoài)**: Nộp CV qua hệ thống

---

## 2. Kiến trúc

### Kết luận: **Modular Monolith + DDD (Domain-Driven Design)**

**Bằng chứng trong code:**

**1. Maven Multi-module (Modular Monolith):**
```
hcm-talent-management-parent (root pom)
├── hcm-talent-management-api     ← Contracts (interfaces, DTOs)
├── hcm-talent-management         ← Business logic (DDD core)
└── hcm-talent-management-app     ← Spring Boot runner (assembly)
```

**2. DDD rõ ràng trong package structure:**
```
com.evotek.talent
├── domain/           ← DOMAIN LAYER (pure business)
│   ├── aggregate/    ← Aggregate Root
│   ├── command/      ← Command objects
│   ├── event/        ← Domain Events
│   ├── exception/    ← Domain Errors
│   ├── query/        ← Query objects
│   └── repository/   ← Repository interfaces (port)
├── application/      ← APPLICATION LAYER
│   ├── dto/          ← Request/Response DTOs
│   ├── mapper/       ← DTO ↔ Domain mapping
│   └── service/      ← Application Services (CQRS pattern)
│       ├── command/  ← Command handlers
│       └── query/    ← Query handlers
├── infrastructure/   ← INFRASTRUCTURE LAYER (adapter)
│   ├── adapter/      ← Implements domain repository interfaces
│   ├── client/       ← External AI client (OkHttp)
│   ├── mapper/       ← Entity ↔ Domain mapping (MapStruct)
│   ├── messaging/    ← RabbitMQ config
│   └── persistence/  ← JPA Entities, Repositories
└── presentation/     ← PRESENTATION LAYER
    ├── rest/         ← REST controllers
    └── messaging/    ← Message consumers (RabbitMQ)
```

**3. Aggregate Root** rõ ràng (extends `ApprovalAggregateRoot`, `PolicyAggregateRoot`).

**4. CQRS Pattern**: Service tách thành `CommandService` và `QueryService`.

**5. Repository port/adapter**: Domain định nghĩa interface `RecruitmentRequestDomainRepository`, Infrastructure implement bằng `RecruitmentRequestAdapter`.

---

## 3. Phân hệ (Module)

### 3.1 Maven modules

| Module | Vai trò |
|---|---|
| `hcm-talent-management-api` | Contract module — chỉ chứa interface và common model |
| `hcm-talent-management` | Core module — toàn bộ business logic và infrastructure |
| `hcm-talent-management-app` | Assembly module — Spring Boot main class, config, chạy application |

### 3.2 Business modules trong `hcm-talent-management`

| Module | Chức năng | Business |
|---|---|---|
| **RecruitmentRequest** | Yêu cầu tuyển dụng | Tạo/duyệt/đóng/mở lại yêu cầu; quản lý documents, criteria, rounds, screening questions |
| **RecruitmentProposal** | Đề xuất tuyển dụng | Giai đoạn đề xuất trước khi tạo recruitment request |
| **Candidate** | Ứng viên trong một đợt tuyển | Assign CV vào recruitment, track trạng thái từng ứng viên |
| **Interview** | Phỏng vấn | Lên lịch, thực hiện, đánh giá phỏng vấn từng vòng |
| **Applicant** | Hồ sơ ứng viên hệ thống | Quản lý ứng viên, gắn hashtag, câu hỏi sàng lọc |
| **CurriculumVitae** | CV của ứng viên | Quản lý CV, học vấn, kinh nghiệm làm việc |
| **EvaluationRound** | Vòng đánh giá | Quản lý các vòng phỏng vấn mẫu (template) |
| **EvaluationCriteria** | Tiêu chí đánh giá | Tiêu chí chấm điểm phỏng vấn |
| **Hashtag** | Nhãn phân loại | Gắn tag vào ứng viên để lọc nhanh |
| **ScreeningQuestion** | Câu hỏi sàng lọc | Câu hỏi dùng chung, gắn vào recruitment request |
| **OnboardTaskTemplate** | Template onboard | Task mẫu khi ứng viên được nhận việc |
| **Comment** | Bình luận | Comment trên hồ sơ ứng viên/ứng tuyển |
| **CandidateEmailLog** | Log email ứng viên | Lịch sử email gửi cho ứng viên |
| **Report** | Báo cáo | Thống kê tỉ lệ CV conversion, báo cáo theo năm |
| **RecruitmentAi** | AI Matching | Gợi ý ứng viên / job phù hợp qua AI external service |

### 3.3 Sơ đồ quan hệ module

```
                    ┌─────────────────────┐
                    │  RecruitmentRequest  │ ← Module lõi
                    └──────────┬──────────┘
                               │ chứa
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
    ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐
    │   Candidate  │  │  EvalCriteria│  │ RecruitmentEvalRound  │
    └──────┬───────┘  └──────────────┘  └──────────────────────┘
           │ chứa               ▲                  ▲
    ┌──────▼───────┐    dùng từ EvaluationRound     │
    │  Interview   │◄──────── (template) ───────────┘
    └──────┬───────┘
           │ gắn vào
    ┌──────▼────────┐       ┌──────────────┐
    │ EvalAttendee  │       │  CurriculumVitae│
    └───────────────┘       └───────┬────────┘
                                    │ thuộc về
                            ┌───────▼────────┐
                            │   Applicant    │◄── Hashtag
                            └────────────────┘◄── ScreeningQuestion

    RecruitmentProposal ──► RecruitmentRequest (giai đoạn trước)

    OnboardTaskTemplate ─── (dùng khi Candidate ONBOARDED)

    CandidateEmailLog ────► Candidate (log email)

    Comment ─────────────► Applicant (bình luận hồ sơ)
```

### 3.4 External dependencies (HCM ecosystem)

```
hcm-talent-management
    ├── → hcm-iam-api          (Identity & Access Management)
    ├── → hcm-notification-api  (Thông báo)
    ├── → hcm-organization-api  (Tổ chức, phòng ban)
    ├── → hcm-pa-api            (Personnel Administration)
    ├── → hcm-pp-api            (Personnel Planning / Job position)
    └── → hcm-storage-api       (Lưu trữ file/CV)
```

---

## 4. Cấu trúc thư mục

```
com.evotek.talent
│
├── domain/                     ← KHÔNG được import infrastructure
│   ├── aggregate/              ← Aggregate Root + Entity + Value Object
│   │   └── enums/              ← Enums thuộc domain (Status, WorkType...)
│   ├── command/                ← Command object (input cho domain method)
│   ├── event/                  ← Domain Events (InterviewEvaluatedEvent...)
│   ├── exception/              ← BadRequestError, NotFoundError (enum-based)
│   ├── query/                  ← Query object (filter params cho repo)
│   └── repository/             ← Port interfaces (DomainRepository)
│
├── application/                ← Orchestration layer
│   ├── dto/
│   │   ├── request/            ← Request DTO từ HTTP/internal
│   │   └── response/           ← Response DTO trả về client
│   ├── mapper/                 ← Request DTO → Command (MapStruct)
│   └── service/
│       ├── command/            ← Interface: CommandService
│       │   └── impl/           ← Implementation: gọi domain + save
│       ├── query/              ← Interface: QueryService
│       │   └── impl/           ← Implementation: gọi repo + map DTO
│       └── impl/               ← Shared services (TalentShareService)
│
├── infrastructure/             ← Kết nối thế giới ngoài
│   ├── adapter/                ← Implements DomainRepository interfaces
│   ├── client/                 ← External HTTP (AI service via OkHttp)
│   │   └── dto/                ← DTO cho external API response
│   ├── mapper/                 ← Entity ↔ Domain (MapStruct)
│   ├── messaging/              ← RabbitMQ bean config
│   ├── persistence/
│   │   ├── entity/             ← JPA Entity (map to DB table)
│   │   ├── readmodel/          ← Read model cho query phức tạp
│   │   └── repository/         ← Spring Data JPA + Custom JPQL
│   │       ├── custom/         ← Custom repository interface
│   │       └── impl/           ← JPQL dynamic query implementation
│   └── support/
│       └── constants/          ← Constants, TalentConstants
│
├── presentation/               ← Entry points
│   ├── rest/                   ← Interface controller (Swagger annotations)
│   │   └── impl/               ← @RestController implementation
│   └── messaging/              ← @RabbitListener consumers
│
└── common/                     ← Shared utilities trong module này
    ├── service/                ← ShareService, TalentShareService
    └── utils/                  ← SplitEmailUtils
```

### Giải thích từng package

| Package | Vai trò |
|---|---|
| `domain/aggregate` | **Trái tim của hệ thống.** Chứa business logic thật sự. Setters private, chỉ expose method domain. |
| `domain/command` | Command object = "đơn đặt hàng" cho Aggregate. Aggregate nhận vào command và thực hiện state change. |
| `domain/repository` | **Port (interface).** Domain không biết JPA là gì — chỉ biết contract. |
| `application/service/command` | Orchestrate: validate → build command → call aggregate → save. **@Transactional** ở đây. |
| `application/service/query` | Đọc dữ liệu, map sang DTO, không modify state. |
| `infrastructure/adapter` | **Adapter (implements port).** Cầu nối giữa domain repo interface và JPA repo thật sự. |
| `infrastructure/persistence/entity` | JPA entity — map 1:1 với DB table. Không chứa business logic. |
| `infrastructure/persistence/repository` | Spring Data JPA + custom JPQL query. |
| `infrastructure/mapper` | MapStruct: `Entity ↔ Domain object`. |
| `application/mapper` | MapStruct: `Request DTO → Command object`. |
| `application/dto` | Data transfer giữa controller và service. |
| `presentation/rest` | Interface định nghĩa API endpoints (Swagger docs ở đây). |
| `presentation/rest/impl` | Controller thật: inject CommandService + QueryService, wrap response. |
| `presentation/messaging` | RabbitMQ consumer — nghe event từ queue, xử lý async. |
| `infrastructure/messaging` | Khai báo Queue, Exchange, Binding bean cho RabbitMQ. |
| `infrastructure/client` | HTTP client gọi external AI service (OkHttp). |
| `domain/exception` | Error codes dạng enum — không throw raw exception string. |

---

## 5. Flow Request

### 5.1 Flow HTTP Request chuẩn

```
Client (HTTP Request)
        │
        ▼
[ presentation/rest/XxxResource.java ]       ← Interface (Swagger docs, @PreAuthorize)
        │
        ▼
[ presentation/rest/impl/XxxResourceImpl.java ]  ← @RestController
        │ gọi
        ├──────────────────────────────────────────────────────────┐
        ▼ (write)                                                  ▼ (read)
[ application/service/command/XxxCommandService ]   [ application/service/query/XxxQueryService ]
        │                                                          │
        │ 1. validate (gọi shareService → external HCM)           │ 1. build Query object
        │ 2. build Command object                                  │ 2. gọi EntityRepository (JPQL)
        │ 3. new Aggregate(cmd) hoặc aggregate.method(cmd)        │ 3. map Entity → DTO
        │ 4. domainRepository.save(aggregate)                      │ 4. enrich (gọi external nếu cần)
        │                                                          │ 5. return DTO
        ▼                                                          │
[ domain/aggregate/XxxAggregate ]               ◄─────────────────┘
        │ state change, validate business rules
        ▼
[ domain/repository/XxxDomainRepository ]  ← Interface (port)
        │
        ▼
[ infrastructure/adapter/XxxAdapter ]      ← Implements port
        │ 1. mapper.toEntity(domain)
        │ 2. entityRepository.save(entity)
        │ 3. save child collections (criteria, rounds...)
        │ 4. return domain object
        ▼
[ infrastructure/persistence/repository/XxxEntityRepository ]  ← Spring Data JPA
        │
        ▼
    Database (PostgreSQL)
        │
        ▼
[ infrastructure/mapper/XxxEntityMapper ]  ← MapStruct: Entity → Domain
        │
        ▼
[ application/mapper/XxxDTOMapper ]        ← MapStruct: Domain → Response DTO
        │
        ▼
    Response (JSON)
```

### 5.2 Flow Async (RabbitMQ)

```
External event (interview.evaluated)
        │
        ▼
[ infrastructure/messaging/TalentRabbitMQConfig ]   ← Queue/Exchange declaration
        │
        ▼
[ presentation/messaging/InterviewEvaluatedConsumer ]  ← @RabbitListener
        │ 1. deserialize CloudEventEnvelope → InterviewEvaluatedEvent
        │ 2. load Interview domain object
        │ 3. load Candidate domain object
        │ 4. candidate.updateStatusInterview(interview)
        │ 5. save Candidate entity
        ▼
    Candidate status updated (PASS/FAIL/INTERVIEW/ONBOARDED)
```

**Trách nhiệm từng bước:**

| Bước | Trách nhiệm |
|---|---|
| Controller/Resource | Nhận request, validate format (@Valid), check permission (@PreAuthorize) |
| CommandService | Orchestrate workflow: validate business context, build command, call domain, save |
| QueryService | Đọc dữ liệu, map DTO, không modify state |
| Aggregate | Enforce business rules, state machine transitions |
| DomainRepository (interface) | Contract: save, getById, enrich |
| Adapter | Bridge: gọi EntityRepository, convert domain ↔ entity |
| EntityRepository | JPA CRUD + custom JPQL query |
| EntityMapper | MapStruct conversion entity ↔ domain |
| DTOMapper | MapStruct conversion domain ↔ DTO |

---

## 6. Phân tích Domain (DDD)

### Domain: **Talent Acquisition (Tuyển dụng)**

### Aggregates và cấu trúc

#### `RecruitmentRequest` (extends `ApprovalAggregateRoot`)
- **Là trung tâm chính của domain**
- Chứa: `List<RecruitmentEvaluationCriteria>`, `List<RecruitmentEvaluationRound>`, `List<RecruitmentScreeningQuestion>`, `List<Candidate>`
- State machine: `NEW → WAIT_APPROVE → APPROVED (OPEN) → CLOSED`
- Approval flow: `NEW → WAIT_APPROVE → APPROVED/REJECTED`
- Business rules trong method: `update()` không cho sửa khi APPROVED/WAIT_APPROVE, `reopen()` chỉ khi APPROVED

#### `Candidate` (extends `PolicyAggregateRoot`)
- Chứa: `List<Interview>`
- State machine phức tạp: `WAIT_INTERVIEW → INTERVIEW → PASS/FAIL → IN_PROPOSAL → APPROVED/REJECTED → ONBOARDED`
- Business method: `createRoundInterview()`, `updateStatusInterview()`, `proposeCandidate()`, `approveCandidate()`

#### `Interview` (aggregate member)
- Mỗi candidate có nhiều interview (nhiều vòng)
- Gắn với `EvaluationRound` (template vòng phỏng vấn)
- Chứa: `List<EvaluationAttendee>`, `List<EvaluationAnswer>`, `InterviewFile`

#### `CurriculumVitae` (Aggregate Root)
- CV của ứng viên
- Chứa: `List<WorkExperience>`, `List<Attachment>` (file CV)

#### `Applicant` (Aggregate Root)
- Ứng viên trong hệ thống
- Chứa: `List<ApplicantHashtag>`, `List<ApplicantScreeningQuestion>`

#### Các Aggregate Root khác
- `RecruitmentProposal` — đề xuất tuyển dụng
- `ScreeningQuestion` — câu hỏi sàng lọc (master data)
- `EvaluationCriteria` — tiêu chí đánh giá (master data)
- `EvaluationRound` — vòng phỏng vấn mẫu (master data)
- `OnboardTaskTemplate` — template onboard (master data)
- `Hashtag` — nhãn phân loại (master data)

### Entities (không phải Aggregate Root)
- `RecruitmentEvaluationCriteria` — criteria của một recruitment request
- `RecruitmentEvaluationRound` — round của một recruitment request
- `RecruitmentScreeningQuestion` — screening question của một recruitment
- `RecruitmentRequestDocument` — file đính kèm recruitment
- `EvaluationAttendee` — người tham gia phỏng vấn
- `EvaluationAnswer` — câu trả lời đánh giá
- `InterviewFile` — file phỏng vấn
- `OnboardTask` — task onboard thực tế
- `WorkExperience` — kinh nghiệm làm việc trong CV

### Value Objects
- Không có Value Object tường minh (không có class `@ValueObject`). Enums được dùng thay thế: `CandidateStatus`, `RecruitmentRequestStatus`, `InterviewStatus`, `WorkType`, `ExperienceLevel`, v.v.

### Domain Events
| Event | Trigger |
|---|---|
| `InterviewEvaluatedEvent` | Interview được hoàn thành đánh giá |
| `InterviewNoticeEvent` | Interview được tạo/huỷ/cập nhật |
| `CandidateCancelledEvent` | Candidate bị huỷ |

### Repository Interfaces (Ports)
- `RecruitmentRequestDomainRepository`
- `CandidateDomainRepository` (không thấy file — có thể qua adapter trực tiếp)
- `InterviewDomainRepository`
- `CurriculumVitaeDomainRepository`
- `ApplicantDomainRepository`
- v.v.

### Nhận xét DDD
- ✅ Aggregate Root có rich domain model (không phải anemic)
- ✅ Business rules được đặt trong domain method
- ✅ Setter private, chỉ expose behavior
- ⚠️ Một số command service vẫn bypass domain, gọi thẳng EntityRepository (xem `ensureRecruitmentRequest`)
- ⚠️ Chưa có Value Object tường minh

---

## 7. Dependency

### Module phụ thuộc

```
hcm-talent-management-app
    └── depends on → hcm-talent-management

hcm-talent-management
    └── depends on → hcm-talent-management-api
    └── depends on → common-util, common-model, common-web, common-persistence,
                     common-cache, common-job, common-workflow, common-export, common-messaging
    └── depends on → hcm-iam-api, hcm-notification-api, hcm-organization-api,
                     hcm-pa-api, hcm-pp-api, hcm-storage-api

hcm-talent-management-api
    └── depends on → common-model
```

### Dependency trong nội bộ module

```
presentation ──────► application (service interfaces)
                         │
                         ├──► domain (aggregate, command, query)
                         │
                         └──► infrastructure (via domain repo interface)

infrastructure ──────► domain (aggregate, repository interfaces)

domain ─────────────► (không depend vào layer nào khác)  ✅
```

### Circular dependency?
**Không có circular dependency** — architecture tuân thủ đúng Dependency Rule của Clean Architecture:
- `domain` không import `application`, `infrastructure`, `presentation`
- `application` không import `infrastructure` trực tiếp (qua domain repository interface)
- ⚠️ **Ngoại lệ**: `RecruitmentRequestCommandServiceImpl` import trực tiếp `RecruitmentRequestEntityRepository` và `RecruitmentRequestEntityMapper` — đây là vi phạm nhỏ của DDD

### Module lõi vs hỗ trợ

| Module | Loại | Lý do |
|---|---|---|
| `RecruitmentRequest` | **Lõi** | Tất cả flow tuyển dụng xoay quanh nó |
| `Candidate` | **Lõi** | Core của tracking ứng viên |
| `Interview` | **Lõi** | Business phức tạp nhất (multi-round) |
| `EvaluationRound/Criteria` | Hỗ trợ | Master data, template |
| `Hashtag/ScreeningQuestion` | Hỗ trợ | Catalog/lookup data |
| `OnboardTaskTemplate` | Hỗ trợ | Post-recruitment |
| `Report` | Hỗ trợ | Reporting only |
| `RecruitmentAi` | Hỗ trợ | Feature phụ, external AI |

---

## 8. Database

> **Lưu ý**: Không có file migration Liquibase trong source đọc được. Phân tích dựa trên JPA Entity.

### Các bảng chính

| Bảng | Entity tương ứng | Mô tả |
|---|---|---|
| `recruitment_request` | `RecruitmentRequestEntity` | Yêu cầu tuyển dụng |
| `recruitment_evaluation_criteria` | `RecruitmentEvaluationCriteriaEntity` | Tiêu chí đánh giá của recruitment |
| `recruitment_evaluation_round` | `RecruitmentEvaluationRoundEntity` | Vòng phỏng vấn của recruitment |
| `recruitment_screening_question` | `RecruitmentScreeningQuestionEntity` | Câu hỏi sàng lọc của recruitment |
| `recruitment_document` | `RecruitmentRequestDocumentEntity` | File đính kèm |
| `candidate` | `CandidateEntity` | Ứng viên trong đợt tuyển |
| `interview` | `InterviewEntity` | Phỏng vấn |
| `evaluation_attendee` | `EvaluationAttendeeEntity` | Người tham gia phỏng vấn |
| `evaluation_answer` | `EvaluationAnswerEntity` | Câu trả lời đánh giá |
| `curriculum_vitae` | `CurriculumVitaeEntity` | CV ứng viên |
| `work_experience` | `WorkExperienceEntity` | Kinh nghiệm làm việc |
| `applicant` | `ApplicantEntity` | Hồ sơ ứng viên |
| `applicant_hashtag` | `ApplicantHashtagEntity` | Tag của ứng viên |
| `applicant_screening_question` | `ApplicantScreeningQuestionEntity` | Câu hỏi sàng lọc của ứng viên |
| `hashtag` | `HashtagEntity` | Master data hashtag |
| `screening_question` | `ScreeningQuestionEntity` | Master data câu hỏi |
| `evaluation_round` | `EvaluationRoundEntity` | Master data vòng phỏng vấn |
| `evaluation_criteria` | `EvaluationCriteriaEntity` | Master data tiêu chí |
| `recruitment_proposal` | `RecruitmentProposalEntity` | Đề xuất tuyển dụng |
| `onboard_task_template` | `OnboardTaskTemplateEntity` | Template onboard task |
| `onboard_task` | `OnboardTaskEntity` | Task onboard thực tế |
| `comment` | `CommentEntity` | Bình luận |
| `candidate_email_log` | `CandidateEmailLogEntity` | Log email |
| `talent_file` | `TalentFileEntity` | File liên quan |

### ERD (Text) — Core Tables

```
recruitment_request
│   id (PK)
│   code, name, status, approval_status
│   requester_id, in_charge, job_position_id, organization_unit_id
│   start_at, end_at, deleted
│
├──< recruitment_evaluation_criteria (recruitment_request_id FK)
│       id, name, deleted
│
├──< recruitment_evaluation_round (recruitment_request_id FK)
│       id, evaluation_round_id (FK → evaluation_round), step, type, deleted
│
├──< recruitment_screening_question (recruitment_request_id FK)
│       id, question, deleted
│
├──< recruitment_document (recruitment_request_id FK)
│       id, document_id (FK → storage system), deleted
│
└──< candidate (recruitment_request_id FK)
        id, curriculum_vitae_id (FK → curriculum_vitae)
        applicant_id (FK → applicant)
        status, approval_status, assignee_worker_id, worker_id
        current_interview_id (FK → interview), is_final_interview, deleted
        │
        └──< interview (candidate_id FK)
                id, evaluation_round_id (FK), step
                status, result_status, in_charge_worker_id, deleted
                │
                ├──< evaluation_attendee (interview_id FK)
                │       id, worker_id, role, type
                │
                └──< evaluation_answer (interview_id FK)
                        id, criteria_id, answer, score

curriculum_vitae
│   id (PK)
│   applicant_id (FK → applicant)
│   in_charge (worker_id)
│   deleted
│
└──< work_experience (curriculum_vitae_id FK)

applicant
│   id (PK)
│   full_name, email, phone, deleted
│
├──< applicant_hashtag (applicant_id FK)
│       hashtag_id (FK → hashtag)
│
└──< applicant_screening_question (applicant_id FK)
        screening_question_id (FK → screening_question)

evaluation_round (master)
    id, name, type (INTERVIEW/WRITTEN_TEST/...), status

evaluation_criteria (master)
    id, name, status
```

### Luồng dữ liệu chính

```
1. HR tạo recruitment_request
2. Thêm evaluation_criteria + evaluation_round vào recruitment
3. Approve recruitment → status OPEN
4. Applicant nộp CV → curriculum_vitae + work_experience created
5. HR assign CV → candidate record created + interview records (1 per round)
6. Interviewer thực hiện interview → evaluation_answer, evaluation_attendee saved
7. Interview complete → event published → candidate status updated
8. Candidate PASS all rounds → isFinalInterview = true
9. Propose candidate → status IN_PROPOSAL
10. Approve candidate → status APPROVED → ONBOARDED
```

---

## 9. Security

### Không có Security config trong source hiện tại

**Nhận xét có cơ sở:**
- `@PreAuthorize("hasPermission(null, 'recruitment_request.create')")` — tức là đang dùng `PermissionEvaluator` custom
- `SecurityUtils.getWorkerId()`, `SecurityUtils.getPiIds()` — custom security context holder
- `PolicyResourceScopeEntity` — hệ thống phân quyền theo resource scope (ABAC — Attribute-Based Access Control)

### JWT/Auth flow (suy luận từ common-web)
```
Request → JWT Filter (từ common-web library)
        → Parse JWT → set SecurityContext
        → SecurityContext chứa: workerId, piIds (permission identifiers)
        → @PreAuthorize gọi custom PermissionEvaluator
        → PermissionEvaluator check PolicyResourceScope trong DB
        → Allow/Deny
```

### Resource-based Authorization
- Mỗi `RecruitmentRequest`, `Candidate` có `PolicyResourceScope` trong DB
- Ghi nhận ai có quyền VIEW/EDIT/DELETE từng record cụ thể
- Query có filter: `AND exists(select prs from PolicyResourceScopeEntity prs where prs.piId IN :piIds)`

> **Không đủ dữ liệu** để phân tích chi tiết JWT filter và login flow — cần xem `common-web` module.

---

## 10. API

### RecruitmentRequest (`/api/v1/recruitments`)
```
POST   /recruitments                                    ← Create
POST   /recruitments/{id}/update                        ← Update
POST   /recruitments/{id}/delete                        ← Delete (soft)
POST   /recruitments/{id}/request-approval              ← Gửi duyệt
POST   /recruitments/{id}/approve                       ← Duyệt
POST   /recruitments/{id}/reject-approval               ← Từ chối
POST   /recruitments/{id}/cancel-approval               ← Huỷ gửi duyệt
POST   /recruitments/{id}/close                         ← Đóng
POST   /recruitments/{id}/reopen                        ← Mở lại
GET    /recruitments/{id}                               ← Get by ID
GET    /recruitments                                    ← Search/paging
GET    /recruitments/auto-complete                      ← Autocomplete
GET    /recruitments/approval-status-statistic          ← Thống kê
POST   /recruitments/{id}/documents                     ← Thêm document
POST   /recruitments/{id}/documents/{docId}/delete      ← Xoá document
POST   /recruitments/{id}/criteria                      ← Thêm criteria
POST   /recruitments/{id}/criteria/{id}/update          ← Update criteria
POST   /recruitments/{id}/criteria/{id}/delete          ← Xoá criteria
POST   /recruitments/{id}/rounds                        ← Thêm round
POST   /recruitments/{id}/rounds/{id}/update            ← Update round
POST   /recruitments/{id}/rounds/{id}/delete            ← Xoá round
GET    /recruitments/{id}/rounds/find-all-by-recruitmentId
POST   /recruitments/{id}/screening-questions           ← Thêm screening question
POST   /recruitments/{id}/screening-questions/{id}/delete
GET    /recruitments/{id}/candidates                    ← Danh sách candidate
POST   /recruitments/{id}/candidates/{cId}/delete       ← Xoá candidate
POST   /recruitments/sync-resource-scope                ← Admin sync
```

### RecruitmentProposal (`/api/v1/recruitment-proposals`)
```
POST   /recruitment-proposals
POST   /recruitment-proposals/{id}/update
POST   /recruitment-proposals/{id}/delete
POST   /recruitment-proposals/{id}/request-approval
POST   /recruitment-proposals/{id}/approve
POST   /recruitment-proposals/{id}/reject-approval
GET    /recruitment-proposals/{id}
GET    /recruitment-proposals
```

### Applicant (`/api/v1/applicants`)
```
POST   /applicants
POST   /applicants/{id}/update
POST   /applicants/{id}/delete
GET    /applicants/{id}
GET    /applicants
POST   /applicants/{id}/hashtags
POST   /applicants/{id}/hashtags/{hId}/delete
POST   /applicants/{id}/screening-questions
POST   /applicants/{id}/screening-questions/{sqId}/update
```

### CurriculumVitae (`/api/v1/curriculum-vitaes`)
```
POST   /curriculum-vitaes
POST   /curriculum-vitaes/{id}/update
POST   /curriculum-vitaes/{id}/delete
GET    /curriculum-vitaes/{id}
GET    /curriculum-vitaes
POST   /curriculum-vitaes/{id}/assign-to-recruitment  ← Assign CV vào recruitment
```

### Candidate (`/api/v1/candidates`)
```
GET    /candidates
GET    /candidates/{id}
POST   /candidates/{id}/cancel
POST   /candidates/{id}/approve
POST   /candidates/{id}/reject
POST   /candidates/{id}/send-wait-approve
POST   /candidates/batch-cancel
POST   /candidates/{id}/propose
```

### Interview (`/api/v1/interviews`)
```
POST   /interviews/{id}/update
POST   /interviews/{id}/cancel
POST   /interviews/{id}/complete
POST   /interviews/{id}/perform-evaluation
POST   /interviews/{id}/auto-evaluation
GET    /interviews/{id}
GET    /interviews
POST   /interviews/{id}/attendees
POST   /interviews/{id}/attendees/{aId}/delete
```

### EvaluationRound (`/api/v1/evaluation-rounds`)
```
POST   /evaluation-rounds
POST   /evaluation-rounds/{id}/update
POST   /evaluation-rounds/{id}/delete
GET    /evaluation-rounds/{id}
GET    /evaluation-rounds
```

### EvaluationCriteria (`/api/v1/evaluation-criteria`)
```
POST   /evaluation-criteria
POST   /evaluation-criteria/{id}/update
POST   /evaluation-criteria/{id}/delete
GET    /evaluation-criteria/{id}
GET    /evaluation-criteria
```

### ScreeningQuestion (`/api/v1/screening-questions`)
```
POST   /screening-questions
POST   /screening-questions/{id}/update
GET    /screening-questions
```

### Hashtag (`/api/v1/hashtags`)
```
POST   /hashtags
POST   /hashtags/{id}/update
GET    /hashtags
```

### OnboardTaskTemplate (`/api/v1/onboard-task-templates`)
```
POST   /onboard-task-templates
POST   /onboard-task-templates/{id}/update
GET    /onboard-task-templates
```

### Comment (`/api/v1/comments`)
```
POST   /comments
POST   /comments/{id}/update
GET    /comments
```

### CandidateEmailLog (`/api/v1/candidate-email-logs`)
```
GET    /candidate-email-logs
```

### Report (`/api/v1/reports`)
```
GET    /reports/cv-conversion-rate
GET    /reports/by-year
```

### RecruitmentAi (`/api/v1/recruitment-ai`)
```
POST   /recruitment-ai/suggest-candidates    ← AI gợi ý ứng viên
POST   /recruitment-ai/suggest-jobs          ← AI gợi ý job phù hợp
```

---

## 11. Điểm cần đọc trước (2 ngày onboarding)

### Ngày 1: Hiểu kiến trúc và domain core

```
Thứ tự    File / Class                               Lý do
──────    ──────────────────────────────────────     ────────────────────────────────
1         pom.xml (root + 3 module)                  Hiểu cấu trúc maven multi-module
2         HcmTalentManagementApplication.java         Entry point Spring Boot
3         TalentRabbitMQConfig.java                   Hiểu messaging topology
4         domain/aggregate/RecruitmentRequest.java    Aggregate Root chính, state machine
5         domain/aggregate/Candidate.java             Aggregate phức tạp thứ 2
6         domain/aggregate/Interview.java             Vòng phỏng vấn
7         domain/exception/BadRequestError.java       Hiểu error domain
8         domain/repository/ (các interface)          Hiểu ports
```

### Ngày 2: Hiểu flow từ API → DB

```
Thứ tự    File / Class                               Lý do
──────    ──────────────────────────────────────     ────────────────────────────────
9         presentation/rest/RecruitmentRequestResource.java       API contract + Swagger
10        presentation/rest/impl/RecruitmentRequestResourceImpl   Controller mỏng
11        application/service/command/impl/RecruitmentRequestCommandServiceImpl  Flow write
12        application/service/query/impl/RecruitmentRequestQueryServiceImpl      Flow read
13        infrastructure/adapter/RecruitmentRequestAdapter.java   Bridge domain-JPA
14        infrastructure/persistence/entity/RecruitmentRequestEntity.java  DB mapping
15        infrastructure/persistence/repository/impl/RecruitmentRequestEntityRepositoryImpl  Dynamic JPQL
16        application/mapper/HrCommandMapper.java                 Request → Command
17        infrastructure/mapper/RecruitmentRequestEntityMapper.java  Entity ↔ Domain
18        application/dto/response/RecruitmentRequestSearchDTO.java  Response structure
19        presentation/messaging/InterviewEvaluatedConsumer.java  Async flow
20        application/service/impl/TalentShareServiceImpl.java    External service calls
```

---

## 12. Đánh giá Code

### ✅ Điểm tốt

| Điểm | Mô tả |
|---|---|
| DDD Architecture | Package structure rõ ràng theo 4 layer: domain, application, infrastructure, presentation |
| Rich Domain Model | Aggregate có business method, không phải anemic model |
| CQRS Pattern | Command/Query service tách biệt — dễ scale và test |
| Setter Private | Domain object setter là private, chỉ expose behavior |
| Error Handling | Domain exception dùng enum (`BadRequestError`) — nhất quán |
| MapStruct | Sử dụng MapStruct đúng cách, tránh mapping thủ công |
| RabbitMQ | DLQ/Parking lot pattern — handle failed message đúng chuẩn |
| Java 21 | Modern Java, dùng pattern matching, stream API |
| Lombok | Code gọn với @Builder, @Getter, @Slf4j |
| Swagger | API được document với @Operation, @Tag |

### ⚠️ Điểm chưa tốt / Code smell

| Vấn đề | Ví dụ | Mức độ |
|---|---|---|
| Layer violation | `RecruitmentRequestCommandServiceImpl` import trực tiếp `RecruitmentRequestEntityRepository` và `RecruitmentRequestEntityMapper` — bypass domain repo | Trung bình |
| God class constructor | Constructor của `RecruitmentRequestCommandServiceImpl` có 12 dependencies | Nhẹ |
| Commented code | `// JobPositionDTO jobPosition = jobPositionList.get(0); ...` trong command service | Nhẹ |
| Missing Value Objects | Salary, Phone, Email là `String` — nên là Value Object | Nhẹ |
| Không có unit test | Folder `test` trống trong module chính | Nghiêm trọng |
| Dynamic JPQL string | String concat JPQL thay vì Criteria API hoặc QueryDSL — risk SQL injection thấp nhưng khó maintain | Trung bình |
| Method đặt tên nhầm | `findCandidateByRecruitment(UUID id, UUID candidateId)` thực ra là **delete** candidate — tên không mô tả đúng | Trung bình |
| Bypass domain trong Consumer | `InterviewEvaluatedConsumer` gọi thẳng `candidateEntityRepository.save()` thay vì qua domain repo | Trung bình |

### Có theo Clean Code không?
- **Phần lớn có**: naming rõ ràng, method nhỏ, single responsibility
- **Chưa hoàn toàn**: một số method quá dài (constructor 12 deps), commented code

### Có theo DDD đúng không?
- **Cơ bản đúng**: Aggregate Root, Command, Event, Repository pattern
- **Chưa đầy đủ**: thiếu Value Object, một số service bypass domain layer

---

## 13. Kết quả cuối — Sơ đồ tổng hợp

### Sơ đồ kiến trúc

```
┌─────────────────────────────────────────────────────────────────┐
│                    hcm-talent-management-app                     │
│  HcmTalentManagementApplication | SwaggerConfig | TaskConfiguration │
└───────────────────────┬─────────────────────────────────────────┘
                        │ depends on
┌───────────────────────▼─────────────────────────────────────────┐
│                   hcm-talent-management                          │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ PRESENTATION                                             │   │
│  │  REST Controllers (interface + impl)                     │   │
│  │  RabbitMQ Consumers (@RabbitListener)                    │   │
│  └───────────────────────┬─────────────────────────────────┘   │
│                           │                                      │
│  ┌───────────────────────▼─────────────────────────────────┐   │
│  │ APPLICATION                                              │   │
│  │  CommandService | QueryService | DTOs | Mappers          │   │
│  └───────────────────────┬─────────────────────────────────┘   │
│                           │                                      │
│  ┌───────────────────────▼─────────────────────────────────┐   │
│  │ DOMAIN (Core — no dependencies)                          │   │
│  │  Aggregates | Commands | Events | Exceptions | Repos     │   │
│  └───────────────────────┬─────────────────────────────────┘   │
│                           │ implements                           │
│  ┌───────────────────────▼─────────────────────────────────┐   │
│  │ INFRASTRUCTURE                                           │   │
│  │  Adapters | JPA Entities | Repositories | External Client│   │
│  │  RabbitMQ Config | MapStruct Mappers                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
         │ depends on API contracts from
┌────────▼────────────────────────────────┐
│          hcm-talent-management-api       │
│  (Service interfaces, shared contracts)  │
└──────────────────────────────────────────┘

External HCM services:
  iam-api | notification-api | organization-api | pa-api | pp-api | storage-api
```

### Sơ đồ module (Business)

```
                    ┌─────────────────┐
                    │ RecruitmentReq  │ ←─── TRUNG TÂM
                    └────────┬────────┘
        ┌───────────┬────────┼────────┬───────────┐
        ▼           ▼        ▼        ▼           ▼
    [Criteria]  [Round]  [Doc]  [Screening]  [Candidate]
                  ▲                              │
                  │                              ▼
            [EvalRound]                     [Interview]
            (master)                            │
                                         [EvalAttendee]
                                         [EvalAnswer]

    [CurriculumVitae] ──► [Candidate]
    [Applicant] ──────────────────────► [CurriculumVitae]
    [Hashtag] ──────────► [Applicant]
    [ScreeningQ] ────────► [Applicant]

    [RecruitmentProposal] ──(tiền thân)──► [RecruitmentRequest]
    [OnboardTaskTemplate] ──(trigger)────► Candidate ONBOARDED
    [RecruitmentAI] ─────────────────────► gợi ý CV / Job
```

### Sơ đồ Request Flow

```
HTTP Request
    │
    ▼ @PreAuthorize (ABAC)
[Resource Interface] (Swagger + permission)
    │
    ▼
[ResourceImpl] (@RestController)
    │
    ├──────────── write ──────────►  [CommandService]
    │                                     │
    │                               validate + build Cmd
    │                                     │
    │                               [Aggregate].method(cmd)
    │                                     │ business rule
    │                               [DomainRepository].save()
    │                                     │
    │                               [Adapter] → [EntityRepository] → DB
    │
    └──────────── read ───────────► [QueryService]
                                         │
                                    [EntityRepository] (custom JPQL)
                                         │
                                    Entity → Domain → DTO
                                         │
                                    Response
```

### Sơ đồ Database (ERD đơn giản hoá)

```
[recruitment_request] ──< [recruitment_evaluation_criteria]
                      ──< [recruitment_evaluation_round] ──> [evaluation_round]
                      ──< [recruitment_screening_question]
                      ──< [recruitment_document]
                      ──< [candidate] ──< [interview] ──< [evaluation_attendee]
                                                      ──< [evaluation_answer]

[curriculum_vitae] ──< [work_experience]
                   ──< [attachment]
                   ──> [applicant]

[applicant] ──< [applicant_hashtag] ──> [hashtag]
            ──< [applicant_screening_question] ──> [screening_question]
```

---

## Checklist kiến thức cần học để đọc dự án này

### Java / Spring
- [ ] Java 21 (records, sealed classes, var, stream API)
- [ ] Spring Boot 3.2 (auto-configuration, application.yml)
- [ ] Spring Data JPA (Repository, EntityManager, JPQL)
- [ ] Spring Web (REST, @RestController, @RequestMapping)
- [ ] Spring Security (@PreAuthorize, PermissionEvaluator)
- [ ] Spring AMQP / RabbitMQ (Queue, Exchange, Binding, @RabbitListener, DLQ)
- [ ] Spring Transaction (@Transactional, isolation, propagation)
- [ ] Lombok (@Getter, @Builder, @Slf4j, @SuperBuilder, @EqualsAndHashCode)
- [ ] MapStruct (code generation cho mapping)
- [ ] Jackson (JSON serialize/deserialize, @JsonProperty)

### Architecture Patterns
- [ ] **DDD** (Domain, Aggregate Root, Entity, Value Object, Repository, Command, Event)
- [ ] **CQRS** (Command Query Responsibility Segregation)
- [ ] **Hexagonal Architecture** (Port & Adapter)
- [ ] **Clean Architecture** (Dependency Rule)
- [ ] **Event-Driven Architecture** (Domain Events, Messaging)

### Design Patterns
- [ ] Repository Pattern
- [ ] Adapter Pattern
- [ ] Builder Pattern
- [ ] Strategy Pattern (nâng cao)
- [ ] State Machine (candidate/recruitment status transitions)

### Infrastructure
- [ ] Maven Multi-module project
- [ ] PostgreSQL (database)
- [ ] RabbitMQ (message broker — queue, exchange, routing key, DLQ)
- [ ] Liquibase (database migration — dùng trong project này)
- [ ] Docker (Dockerfile có trong project)
- [ ] Jenkins (Jenkinsfile có trong project)
- [ ] SonarQube (sonar-project.properties có trong project)

### API / HTTP
- [ ] RESTful API design
- [ ] Swagger/OpenAPI 3 (SpringDoc)
- [ ] JWT (dùng qua common-web)
- [ ] ABAC (Attribute-Based Access Control)

---

> **Mentor note**: Bắt đầu từ `RecruitmentRequest.java` trong domain. Đọc các business method trong đó trước, hiểu state machine của nó. Sau đó trace từ controller xuống đến DB theo 1 flow cụ thể (ví dụ: tạo recruitment request). Đó là cách nhanh nhất.
