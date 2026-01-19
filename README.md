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

## 🤝 Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 👥 Team

- **Developer:** caoanh_ng
- **Mentor:** KMA

## 📞 Contact

- **Email:** nguyenmanhcaoanh9103@gmail.com
- **GitHub:** [github.com/caoanhnguyen](https://github.com/caoanhnguyen)


**Built with ❤️ using Spring Boot**

