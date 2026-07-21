# Exam Online System

A RESTful backend system for managing online examinations, built with **Java Spring Boot**. The system supports authentication, role-based access control (RBAC), exam management, question banks, online test attempts, and reporting.

> This project is designed for schools, training centers, and organizations that need a scalable online examination platform.

---

# Features

- JWT Authentication & Authorization
- Role-Based Access Control (RBAC)
- User & Role Management
- Question Bank Management
- Exam & Test Management
- Online Exam Attempts
- Dashboard Statistics
- RESTful APIs
- Swagger API Documentation

---

# Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 17 |
| Framework | Spring Boot |
| Security | Spring Security, JWT |
| ORM | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| Migration | Flyway |
| Build Tool | Maven |
| API Documentation | Swagger / OpenAPI |
| Deployment | Docker, Render |

---

# System Architecture

```
Client
    │
 REST API
    │
Controller
    │
Service
    │
Repository
    │
PostgreSQL
```

---

# Main Modules

| Module | Description |
|---------|-------------|
| Authentication | Login, JWT, Authorization |
| User Management | CRUD users, assign roles |
| Role Management | Manage system roles |
| Permission Management | RBAC permissions |
| Feature Management | Functional permissions |
| Exam Management | Create and manage exams |
| Test Management | Manage tests inside exams |
| Question Management | Manage question bank |
| Answer Management | Manage answers |
| Classroom Management | Manage classrooms |
| Exam Attempt | Student exam sessions |
| Dashboard | System statistics |

---

# Security

The project implements **Role-Based Access Control (RBAC)**.

```
User
   │
 Role
   │
Permission
   │
Feature
```

Example:

```
ADMIN
    ├── User Management
    ├── Role Management
    ├── Exam Management
    └── Dashboard

TEACHER
    ├── Question Management
    ├── Exam Management
    └── Grade Exams

STUDENT
    ├── Take Exam
    └── View Results
```

---

# API Documentation

Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

---

# Getting Started

## Clone repository

```bash
git clone https://github.com/nngohoangganhh/ExamOnlineSystem.git
```

## Configure Database

Update the database configuration in

```
application.yml
```

Example:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/exam_online
    username: postgres
    password: your_password
```

---

## Run Flyway Migration

The database schema will be created automatically when the application starts.

---

## Start Project

```bash
mvn clean install

mvn spring-boot:run
```

or

```bash
./mvnw spring-boot:run
```

---

# Project Structure

```
src
 ├── config
 ├── security
 ├── controller
 ├── service
 ├── repository
 ├── entity
 ├── dto
 ├── mapper
 ├── exception
 └── util
```

---

# Screenshots

Coming soon.

---

# Deployment

Backend

```
https://your-render-url.onrender.com
```

Swagger

```
https://your-render-url.onrender.com/swagger-ui/index.html
```

---

# Future Improvements

- Unit Testing
- Integration Testing
- Docker Compose
- GitHub Actions CI/CD
- Redis Cache
- Email Notification
- Audit Logging
- Monitoring (Prometheus + Grafana)

---

# Author

Ngô Hoàng Anh

GitHub

https://github.com/nngohoangganhh
