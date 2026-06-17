# 🧪 Hướng dẫn Test API - Hệ Thống Thi Trắc Nghiệm

## Bước 1 — Khởi động Server

Chạy lệnh sau trong terminal tại thư mục dự án:
```bash
.\mvnw.cmd spring-boot:run
```

Chờ đến khi thấy log:
```
Started EduOnlApplication in X.XXX seconds
```

> [!IMPORTANT]
> Đảm bảo **PostgreSQL đang chạy** và database `eduOnl` đã tồn tại trên cổng 5432 với user `postgres` / password `123456`

---

## Bước 2 — Truy cập Swagger UI (Khuyên dùng)

Mở trình duyệt vào: **http://localhost:8080/swagger-ui/index.html**

Tại đây bạn thấy **toàn bộ API**, có thể test trực tiếp mà không cần Postman.

### Cách authorize trên Swagger:
1. Gọi `POST /api/auth/login` → copy giá trị `token` trong response
2. Nhấn nút **Authorize 🔒** ở góc phải trên
3. Nhập: `Bearer eyJhbGci...` (thêm chữ `Bearer ` trước token)
4. Nhấn **Authorize** → **Close**

---

## Bước 3 — Cấu hình Postman

### Tạo Environment `Exam System`

| Variable | Value |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `token` | _(để trống, sẽ tự điền sau login)_ |

### Cấu hình Authorization mặc định cho Collection

- Tab **Authorization** → Type: `Bearer Token`
- Token: `{{token}}`

> Mọi request trong collection sẽ tự dùng token này, không cần nhập lại.

---

## Bước 4 — Trình tự test theo luồng

### 🔐 NHÓM 1: Authentication

#### 1.1 Đăng nhập (lấy token)
```
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin@123"
}
```

**Response mẫu:**
```json
{
  "success": true,
  "status": 200,
  "message": "Đăng nhập thành công",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "admin",
    "role": "ADMIN"
  }
}
```

> [!TIP]
> Trong Postman, vào tab **Tests** của request Login, thêm đoạn này để tự động lưu token:
> ```javascript
> const res = pm.response.json();
> pm.environment.set("token", res.data.token);
> ```

---

#### 1.2 Xem profile
```
GET {{baseUrl}}/api/auth/profile
Authorization: Bearer {{token}}
```

#### 1.3 Đổi mật khẩu
```
PUT {{baseUrl}}/api/auth/change-password
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "oldPassword": "Admin@123",
  "newPassword": "NewPass@456"
}
```

---

### 📋 NHÓM 2: Quản lý Kỳ Thi (Exam)

> [!NOTE]
> Cần quyền `EXAM:READ`, `EXAM:CREATE`, etc. — ADMIN có đủ quyền

#### 2.1 Lấy danh sách kỳ thi
```
GET {{baseUrl}}/api/exams?pageNo=0&pageSize=10
Authorization: Bearer {{token}}
```

#### 2.2 Tạo kỳ thi mới
```
POST {{baseUrl}}/api/exams
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "name": "Kỳ thi Toán cuối kỳ",
  "description": "Thi cuối kỳ môn Toán lớp 10",
  "startTime": "2025-07-01T08:00:00",
  "endTime": "2025-07-01T10:00:00"
}
```

> Lưu `id` trong response để dùng cho bước tiếp theo.

#### 2.3 Gán học sinh vào kỳ thi
```
POST {{baseUrl}}/api/exams/{{examId}}/students
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "studentIds": [2, 3, 4]
}
```

---

### 📝 NHÓM 3: Quản lý Đề Thi (Test)

#### 3.1 Tạo đề thi
```
POST {{baseUrl}}/api/tests
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "name": "Đề Toán 45 phút",
  "examId": 1,
  "duration": 45,
  "totalQuestions": 30,
  "passingScore": 5.0
}
```

#### 3.2 Gán câu hỏi vào đề thi
```
POST {{baseUrl}}/api/tests/{{testId}}/questions
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "questionIds": [1, 2, 3, 4, 5]
}
```

---

### ❓ NHÓM 4: Quản lý Câu Hỏi (Question & Answer)

#### 4.1 Tạo câu hỏi
```
POST {{baseUrl}}/api/questions
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "content": "Giải phương trình: 2x + 4 = 0",
  "level": "EASY",
  "explanation": "x = -2"
}
```

#### 4.2 Thêm đáp án cho câu hỏi
```
POST {{baseUrl}}/api/answers
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "questionId": 1,
  "content": "x = -2",
  "correct": true
}
```

> Tạo thêm 3 đáp án sai: `correct: false`

---

### 🎯 NHÓM 5: Thi & Kết Quả (Student Flow)

> [!IMPORTANT]
> Đăng nhập bằng tài khoản **STUDENT** để test nhóm này

#### 5.1 Xem danh sách kỳ thi được gán
```
GET {{baseUrl}}/api/dashboard/my-exams
Authorization: Bearer {{studentToken}}
```

#### 5.2 Bắt đầu làm bài
```
POST {{baseUrl}}/api/attempts/start/test/{{testId}}
Authorization: Bearer {{studentToken}}
```

**Response trả về:**
```json
{
  "data": {
    "attemptId": 1,
    "questions": [
      {
        "questionId": 1,
        "content": "Giải phương trình: 2x + 4 = 0",
        "answers": [
          {"answerId": 1, "content": "x = -2"},
          {"answerId": 2, "content": "x = 2"},
          ...
        ]
      }
    ]
  }
}
```

#### 5.3 Nộp bài
```
POST {{baseUrl}}/api/attempts/{{attemptId}}/submit
Authorization: Bearer {{studentToken}}
Content-Type: application/json

{
  "answers": [
    {"questionId": 1, "answerId": 1},
    {"questionId": 2, "answerId": 5},
    {"questionId": 3, "answerId": 9}
  ]
}
```

#### 5.4 Xem lịch sử thi
```
GET {{baseUrl}}/api/attempts/my?pageNo=0&pageSize=10
Authorization: Bearer {{studentToken}}
```

#### 5.5 Xem chi tiết kết quả (từng câu đúng/sai)
```
GET {{baseUrl}}/api/attempts/{{attemptId}}
Authorization: Bearer {{studentToken}}
```

---

### 📊 NHÓM 6: Dashboard

#### Admin dashboard
```
GET {{baseUrl}}/api/dashboard/admin
Authorization: Bearer {{token}}
```

#### Student dashboard
```
GET {{baseUrl}}/api/dashboard/student
Authorization: Bearer {{studentToken}}
```

---

## Bước 5 — Import Collection nhanh vào Postman

Bạn có thể dùng **Swagger Import**:
1. Mở Postman → **Import**
2. Chọn tab **Link**
3. Nhập URL: `http://localhost:8080/v3/api-docs`
4. Nhấn **Import** → Postman tự tạo toàn bộ collection!

---

## Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách fix |
|---|---|---|
| `403 Forbidden` | Chưa gửi token hoặc token hết hạn | Login lại để lấy token mới |
| `403 Forbidden` | Đúng token nhưng không đủ quyền | Đăng nhập bằng ADMIN |
| `500 Internal Server Error` | Lỗi server, xem log terminal | Kiểm tra log Spring Boot |
| Connection refused | Server chưa chạy hoặc sai port | Chạy `.\mvnw.cmd spring-boot:run` |
| DB connection failed | PostgreSQL chưa chạy | Khởi động PostgreSQL |
