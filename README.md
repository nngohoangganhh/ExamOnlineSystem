# Exam Online System

Hệ thống thi trắc nghiệm trực tuyến được xây dựng bằng **Java Spring Boot**, hỗ trợ quản lý người dùng, phân quyền, đề thi, câu hỏi, đáp án, bài thi và quá trình làm bài của thí sinh.

Dự án phù hợp cho các hệ thống kiểm tra online trong trường học, trung tâm đào tạo hoặc nội bộ doanh nghiệp.

---

## 1. Mục tiêu hệ thống

Hệ thống Exam Online giúp:

- Quản lý tài khoản người dùng
- Phân quyền theo Role, Feature, Permission
- Quản lý kỳ thi / đề thi / bài test
- Quản lý câu hỏi và đáp án
- Cho phép thí sinh làm bài thi online
- Lưu lại kết quả làm bài
- Cung cấp dữ liệu thống kê cho dashboard

---

## 2. Công nghệ sử dụng

### Backend

- Java 17+
- Spring Boot
- Spring Web
- Spring Security
- JWT Authentication
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven
- Swagger / OpenAPI

### Tools

- IntelliJ IDEA / VS Code
- Postman
- pgAdmin
- Docker
- Render

---

## 3. Các module chính

Dựa trên hệ thống API hiện tại, dự án gồm các controller chính:

| Module | Chức năng |
|---|---|
| `auth-controller` | Đăng nhập, xác thực người dùng, cấp JWT |
| `user-controller` | Quản lý người dùng |
| `role-controller` | Quản lý vai trò |
| `feature-controller` | Quản lý chức năng hệ thống |
| `permission-controller` | Quản lý quyền truy cập |
| `exam-controller` | Quản lý kỳ thi / bài thi |
| `test-controller` | Quản lý test thuộc exam |
| `question-controller` | Quản lý câu hỏi |
| `answer-controller` | Quản lý đáp án |
| `class-controller` | Quản lý lớp học |
| `exam-attempt-controller` | Quản lý lượt làm bài của thí sinh |
| `dashboard-controller` | Thống kê tổng quan hệ thống |


---

## 4. Chức năng chính

### 4.1. Authentication

- Đăng nhập tài khoản
- Sinh JWT token
- Bảo vệ API bằng Spring Security
- Phân quyền truy cập theo role / permission

### 4.2. User Management

- Tạo người dùng
- Cập nhật thông tin người dùng
- Xem danh sách người dùng
- Xem chi tiết người dùng
- Xóa hoặc vô hiệu hóa người dùng
- Gán role cho người dùng

### 4.3. Role & Permission

Hệ thống RBAC gồm các thành phần chính:

- User
- Role
- Feature
- Permission

Ý nghĩa:

- `User`: tài khoản sử dụng hệ thống
- `Role`: vai trò như ADMIN, TEACHER, STUDENT
- `Feature`: chức năng trong hệ thống
- `Permission`: quyền thao tác như VIEW, CREATE, UPDATE, DELETE

Ví dụ:

```text
ADMIN  -> có toàn quyền
TEACHER -> quản lý exam, test, question, answer
STUDENT -> làm bài thi, xem kết quả cá nhân
