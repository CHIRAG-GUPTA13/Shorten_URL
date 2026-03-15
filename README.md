# 🔗 URL Shortener Backend

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.0-6DB33F?style=for-the-badge&logo=spring-boot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-23-ED8B00?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/PostgreSQL-336791?style=for-the-badge&logo=postgresql" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis" alt="Redis">
  <img src="https://img.shields.io/badge/Apache%20Kafka-231F20?style=for-the-badge&logo=apache-kafka" alt="Kafka">
</p>

A production-ready URL shortening service built with Spring Boot, featuring high-performance caching, real-time analytics via Kafka, QR code generation, and robust security.

![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/grass.png)

## ✨ Features

| Feature | Description |
|---------|-------------|
| �_short URL Creation_ | Create custom, random, or user-defined short URLs |
| 📊 _Analytics_ | Track clicks, referrers, device types, and geolocation |
| 📈 _Statistics_ | Summary stats, top performing URLs, and click timelines |
| 🖼️ _QR Code Generation_ | Generate QR codes for any shortened URL |
| 📦 _Bulk Operations_ | Shorten multiple URLs in a single request |
| 🔐 _Authentication_ | Secure JWT-based authentication |
| ⚡ _Rate Limiting_ | Protect API endpoints from abuse |
| 🚀 _High Performance_ | Redis caching for lightning-fast redirects |
| 🔄 _Click Events_ | Kafka-powered real-time event streaming |

![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/grass.png)

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (Angular/React)                │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Boot REST API                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐     │
│  │   Auth   │ │   URL    │ │Analytics │ │  QR Code     │     │
│  │Controller│ │Controller│ │Controller│ │  Controller  │     │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                                  │
            ┌─────────────────────┼─────────────────────┐
            ▼                     ▼                     ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│   PostgreSQL     │  │      Redis       │  │      Kafka      │
│   (Primary DB)   │  │    (Caching)     │  │  (Event Stream)  │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/grass.png)

## 🛠️ Technology Stack

### Backend
- **Framework:** Spring Boot 3.3.0
- **Language:** Java 23
- **Build Tool:** Gradle
- **Security:** Spring Security + JWT

### Database & Caching
- **Primary DB:** PostgreSQL
- **Cache:** Redis
- **ORM:** Spring Data JPA / Hibernate

### Messaging & Services
- **Message Broker:** Apache Kafka
- **QR Codes:** Google ZXing
- **Monitoring:** Spring Actuator

### External Integrations
- **Frontend:** Angular / React

![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/grass.png)

## 📋 Prerequisites

| Requirement | Version |
|-------------|---------|
| Java JDK | 23+ |
| Gradle | 8.5+ |
| PostgreSQL | 14+ |
| Redis | 7.0+ |
| Kafka | 3.5+ |

![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/grass.png)

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd url
```

### 2. Configure Database

Create a PostgreSQL database and update `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/urlshortener
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Configure Redis

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 4. Run the Application

```bash
# Using Gradle
./gradlew bootRun

# Or build and run
./gradlew build
java -jar build/libs/url-0.0.1-SNAPSHOT.jar
```

### 5. Start Infrastructure Services (Optional)

```bash
# Start Kafka, Zookeeper, and Kafka UI
docker-compose up -d
```

The application will be available at: `http://localhost:8080`

![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/grass.png)

## 📚 API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and get JWT token |

### URL Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/urls/shorten` | Create a short URL |
| GET | `/api/urls` | List all URLs for user |
| GET | `/api/urls/{shortCode}` | Get URL details |
| PUT | `/api/urls/{shortCode}` | Update URL |
| DELETE | `/api/urls/{shortCode}` | Delete URL |

### Redirect

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/{shortCode}` | Redirect to original URL |

### Analytics

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/analytics/summary` | Get summary statistics |
| GET | `/api/analytics/top-performing` | Get top URLs by clicks |
| GET | `/api/analytics/url/{shortCode}` | Get URL-specific stats |

### QR Code

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/qr/{shortCode}` | Generate QR code image |

### Bulk Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/bulk/shorten` | Bulk shorten URLs |
| GET | `/api/bulk/status/{jobId}` | Get job status |

### Preferences

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/url-preferences` | Get URL preferences |
| PUT | `/api/url-preferences` | Update preferences |

### Health

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/health/redis` | Redis connectivity check |

![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/grass.png)

## 📝 Request/Response Examples

### Register User
```json
POST /api/auth/register
{
  "email": "user@example.com",
  "password": "securepassword123",
  "username": "johndoe"
}
```

### Shorten URL
```json
POST /api/urls/shorten
{
  "originalUrl": "https://example.com/very-long-url",
  "customCode": "my-custom-code",  // optional
  "expiresInDays": 30              // optional
}
```

### Response
```json
{
  "shortCode": "abc123",
  "shortUrl": "http://localhost:8080/abc123",
  "originalUrl": "https://example.com/very-long-url",
  "createdAt": "2026-03-15T10:30:00Z",
  "expiresAt": "2026-04-14T10:30:00Z"
}
```

![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/grass.png)

## ⚙️ Configuration

### Application Properties

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/urlshortener
spring.datasource.username=postgres
spring.datasource.password=postgres

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Kafka
spring.kafka.bootstrap-servers=localhost:9092

# JWT
jwt.secret=your-super-secret-key
jwt.expiration=86400000

# Rate Limiting
rate-limit.requests-per-minute=60

# URL Settings
url.default-expiry-days=30
url.code-length=6
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | localhost |
| `DB_PORT` | PostgreSQL port | 5432 |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |
| `KAFKA_HOST` | Kafka host | localhost |
| `KAFKA_PORT` | Kafka port | 9092 |

![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/grass.png)

## 🧪 Testing

```bash
# Run unit tests
./gradlew test

# Run integration tests
./gradlew integrationTest

# Run with coverage
./gradlew test jacocoTestReport
```

![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/grass.png)

## 📁 Project Structure

```
url/
├── src/main/java/com/example/demo/
│   ├── UrlApplication.java          # Main entry point
│   └── shortenurl/
│       ├── config/                  # Configuration classes
│       │   ├── SecurityConfig.java
│       │   ├── RedisConfig.java
│       │   ├── KafkaConfig.java
│       │   └── RateLimitConfig.java
│       ├── controller/              # REST controllers
│       │   ├── UrlController.java
│       │   ├── AuthController.java
│       │   ├── AnalyticsController.java
│       │   └── ...
│       ├── service/                 # Business logic
│       ├── repository/              # Data access
│       ├── entity/                  # JPA entities
│       ├── dto/                     # Data transfer objects
│       ├── exception/               # Custom exceptions
│       └── kafka/                   # Kafka producers
├── src/main/resources/
│   └── application.properties
├── src/test/                        # Test files
├── build.gradle
├── docker-compose.yml
└── README.md
```

![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/grass.png)

## 🔧 Development

### Code Generation

The project uses Lombok to reduce boilerplate code. Ensure your IDE supports it:

- **IntelliJ IDEA:** Enable Annotation Processing
- **VS Code:** Install Lombok extension

### Adding New Dependencies

```bash
# Add a new dependency
./gradlew add --name dependency-name
```

Or edit `build.gradle` directly:

```groovy
implementation 'group:artifact:version'
```

![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/grass.png)

## 🚢 Docker Deployment

```bash
# Build the application
./gradlew bootBuildImage

# Run with Docker Compose
docker-compose -f docker-compose.yml up -d
```

![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/grass.png)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ❤️ by the development team
</p>
