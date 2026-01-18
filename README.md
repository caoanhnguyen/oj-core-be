# OJ-Core Backend Service 🚀

**Online Judge Core Service** - Monolithic Web Server cho hệ thống chấm bài lập trình trực tuyến.

## 📌 Tổng quan

OJ-Core là service trung tâm của hệ thống Online Judge, chịu trách nhiệm:
- ✅ Quản lý User & Authentication (JWT + OAuth2)
- ✅ Quản lý Bài tập & Cuộc thi
- ✅ Quản lý Submission & Lịch sử nộp bài
- ✅ Producer/Consumer với RabbitMQ cho việc chấm bài
- ✅ Tích hợp với MySQL, Redis, MinIO, Firebase

## 🏗️ Kiến trúc

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Frontend  │────────▶│   OJ-Core    │◀───────▶│   MySQL     │
│   (Vue 3)   │         │  (Monolith)  │         │  (Database) │
└─────────────┘         └──────────────┘         └─────────────┘
                               │
                    ┌──────────┼──────────┐
                    │          │          │
              ┌─────▼────┐ ┌──▼────┐ ┌──▼─────┐
              │ RabbitMQ │ │ Redis │ │ MinIO  │
              │  (Queue) │ │(Cache)│ │(S3)    │
              └──────────┘ └───────┘ └────────┘
                    │
              ┌─────▼────────┐
              │  OJ-Judge    │
              │  (Worker)    │
              └──────────────┘
```

## 🛠️ Tech Stack

- **Framework:** Spring Boot 4.0.0
- **Language:** Java 17
- **Security:** Spring Security + JWT + OAuth2
- **Database:** MySQL (JPA/Hibernate)
- **Cache:** Redis
- **Message Queue:** RabbitMQ
- **Object Storage:** MinIO
- **Mapping:** MapStruct
- **Build Tool:** Maven

## 📁 Project Structure

Dự án sử dụng **Layered Architecture**:

```
src/main/java/com/kma/ojcore/
├── controller/          # REST API Controllers
├── service/             # Business Logic Layer
├── repository/          # Data Access Layer
├── entity/              # JPA Entities
├── dto/                 # Data Transfer Objects
├── mapper/              # MapStruct Mappers
├── security/            # Security (JWT + OAuth2)
├── config/              # Spring Configuration
├── exception/           # Exception Handling
└── OjCoreApplication.java
```

📖 Chi tiết: [STRUCTURE.md](STRUCTURE.md)

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+
- MinIO (optional)
- RabbitMQ (optional)

### 1. Clone Repository

```bash
git clone <repository-url>
cd oj-core
```

### 2. Configure Environment

Tạo file `.env` trong thư mục root:

```properties
# Database
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=oj_db

# JWT Configuration
JWT_SECRET=your-secret-key-min-32-chars
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000

# OAuth2 Google
OAUTH2_GOOGLE_CLIENT_ID=your-google-client-id
OAUTH2_GOOGLE_CLIENT_SECRET=your-google-client-secret

# OAuth2 GitHub
OAUTH2_GITHUB_CLIENT_ID=your-github-client-id
OAUTH2_GITHUB_CLIENT_SECRET=your-github-client-secret

# Application URLs
APP_URL=http://localhost:8080
FRONTEND_URL=http://localhost:3000
```

### 3. Build & Run

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run
```

Application sẽ chạy tại: `http://localhost:8080`

## 📚 API Documentation

### Authentication Endpoints

#### Register
```bash
POST /api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "fullName": "Test User"
}
```

#### Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "testuser",
  "password": "password123"
}
```

#### Refresh Token
```bash
POST /api/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "your-refresh-token"
}
```

### OAuth2 Endpoints

- **Google Login:** `GET /oauth2/authorization/google`
- **GitHub Login:** `GET /oauth2/authorization/github`

### User Endpoints

#### Get Current User
```bash
GET /api/user/me
Authorization: Bearer {access-token}
```

#### Update User
```bash
PUT /api/user/me
Authorization: Bearer {access-token}
Content-Type: application/json

{
  "fullName": "New Name",
  "bio": "My bio"
}
```

## 🔐 Authentication Flow

### JWT Authentication

```
Client → POST /api/auth/login
       ↓
   Validate credentials
       ↓
   Generate JWT tokens
       ↓
   Return access + refresh token
       ↓
Client stores tokens
       ↓
Client → GET /api/user/me (with Bearer token)
       ↓
   Validate JWT
       ↓
   Return user data
```

### OAuth2 Flow

```
Client → GET /oauth2/authorization/google
       ↓
   Redirect to Google
       ↓
   User logs in
       ↓
   Google redirects back
       ↓
   Load user info
       ↓
   Create/Update user in DB
       ↓
   Generate JWT token
       ↓
   Redirect to frontend with token
```

## 🧪 Testing

### Run Tests
```bash
mvn test
```

### Run with Coverage
```bash
mvn test jacoco:report
```

### Test with Postman

Import collection: `postman/OJ-Core.postman_collection.json`

## 🐳 Docker Support

### Using Docker Compose

```bash
# Start all services (MySQL, Redis, RabbitMQ, MinIO)
docker-compose up -d

# Run application
mvn spring-boot:run
```

### Build Docker Image

```bash
docker build -t oj-core:latest .
docker run -p 8080:8080 oj-core:latest
```

## 📦 Database Schema

### Users Table
- `id` (BIGINT, PK)
- `username` (VARCHAR, UNIQUE)
- `email` (VARCHAR, UNIQUE)
- `password` (VARCHAR)
- `full_name` (VARCHAR)
- `avatar_url` (VARCHAR)
- `bio` (TEXT)
- `provider` (VARCHAR) - LOCAL, GOOGLE, GITHUB
- `provider_id` (VARCHAR)
- `status` (VARCHAR) - ACTIVE, INACTIVE, BANNED
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

### Roles Table
- `id` (BIGINT, PK)
- `name` (VARCHAR) - USER, ADMIN, MODERATOR

### User_Roles Table (Many-to-Many)
- `user_id` (BIGINT, FK)
- `role_id` (BIGINT, FK)

## 🔧 Configuration

### application.yml

```yaml
spring:
  application:
    name: oj-core
  
  datasource:
    url: jdbc:mysql://localhost:3306/oj_db
    username: root
    password: ${MYSQL_ROOT_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${OAUTH2_GOOGLE_CLIENT_ID}
            client-secret: ${OAUTH2_GOOGLE_CLIENT_SECRET}
            scope: profile, email
          github:
            client-id: ${OAUTH2_GITHUB_CLIENT_ID}
            client-secret: ${OAUTH2_GITHUB_CLIENT_SECRET}
            scope: user:email, read:user

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION_MS}
  refresh-expiration: ${JWT_REFRESH_EXPIRATION_MS}
```

## 🤝 Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 👥 Team

- **Developer:** Your Name
- **Mentor:** KMA Instructor

## 📞 Contact

- **Email:** your-email@example.com
- **GitHub:** [github.com/your-username](https://github.com/your-username)

## 📚 Additional Documentation

- [Project Structure](STRUCTURE.md) - Chi tiết cấu trúc dự án
- [Migration Guide](MIGRATION_GUIDE.md) - Hướng dẫn migration
- [Auth Module](AUTH_MODULE_README.md) - Chi tiết về Authentication module

---

**Built with ❤️ using Spring Boot**

