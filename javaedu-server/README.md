# JavaEdu Server - Phase 1 Setup Guide

This guide walks you through setting up and running the JavaEdu backend server.

## Prerequisites

Before starting, make sure you have:

1. **Java 17 JDK** installed
   - Download from: https://adoptium.net/
   - Verify: `java -version` should show 17.x

2. **Maven 3.8+** installed
   - Download from: https://maven.apache.org/download.cgi
   - Verify: `mvn -version`

3. **An IDE** (recommended: IntelliJ IDEA or VS Code with Java extensions)

## Quick Start (Development Mode with H2)

The easiest way to get started is using the `dev` profile which uses an in-memory H2 database (no PostgreSQL needed!).

### Step 1: Navigate to the project

```bash
cd javaedu-server
```

### Step 2: Build the project

```bash
mvn clean compile
```

If this fails, check the error messages. Common issues:
- Wrong Java version: Make sure JAVA_HOME points to Java 17
- Maven not found: Add Maven to your PATH

### Step 3: Run the application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or in Windows PowerShell:
```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### Step 4: Verify it's running

Open a browser or use curl:

```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{"status":"OK","timestamp":"2024-..."}
```

### Step 5: Access H2 Console (optional)

In dev mode, you can browse the database:
1. Open: http://localhost:8080/h2-console
2. JDBC URL: `jdbc:h2:mem:javaedu`
3. Username: `sa`
4. Password: (leave empty)
5. Click Connect

## Running Tests

```bash
mvn test
```

This runs all unit and integration tests using the `test` profile (H2 in-memory database).

## API Endpoints (Phase 1)

Once running, these endpoints are available:

### Health Check
```
GET /api/health
```

### Authentication
```
POST /api/auth/register   - Register a new user
POST /api/auth/login      - Login and get JWT token
POST /api/auth/refresh    - Refresh JWT token
GET  /api/auth/me         - Get current user info (requires auth)
```

### Testing Authentication

1. **Register a user:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Test Teacher\",\"email\":\"teacher@test.com\",\"password\":\"password123\",\"role\":\"TEACHER\"}"
```

2. **Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"teacher@test.com\",\"password\":\"password123\"}"
```

3. **Use the token:**
```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE"
```

## Project Structure

```
javaedu-server/
├── src/
│   ├── main/
│   │   ├── java/com/javaedu/
│   │   │   ├── JavaEduApplication.java    # Main entry point
│   │   │   ├── config/                    # Configuration classes
│   │   │   ├── controller/                # REST controllers
│   │   │   ├── dto/                       # Data transfer objects
│   │   │   ├── exception/                 # Custom exceptions
│   │   │   ├── model/                     # JPA entities
│   │   │   ├── repository/                # Data repositories
│   │   │   ├── security/                  # JWT & auth
│   │   │   ├── service/                   # Business logic
│   │   │   └── sandbox/                   # Code execution sandbox
│   │   └── resources/
│   │       ├── application.yml            # Configuration
│   │       └── db/migration/              # Flyway migrations
│   └── test/                              # Test files
└── pom.xml                                # Maven configuration
```

## Configuration Profiles

| Profile | Database | Use Case |
|---------|----------|----------|
| `dev` | H2 (in-memory) | Local development, no setup needed |
| `test` | H2 (in-memory) | Running automated tests |
| `production` | PostgreSQL | Production deployment |
| (default) | PostgreSQL | Requires DB setup |

## Troubleshooting

### "Port 8080 already in use"
Another application is using port 8080. Either:
- Stop the other application
- Change the port: `mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dserver.port=8081`

### "Java version mismatch"
Make sure JAVA_HOME points to Java 17:
```bash
# Windows
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.x.x

# Mac/Linux
export JAVA_HOME=/path/to/jdk-17
```

### "Cannot find symbol" or compilation errors
Run a clean build:
```bash
mvn clean compile
```

### Tests failing
Run tests with more output:
```bash
mvn test -X
```

## Next Steps

After verifying Phase 1 works:

1. **Run the tests** to make sure everything passes
2. **Try the API endpoints** with curl or Postman
3. **Review the code** in `src/main/java/com/javaedu`
4. Move on to **Phase 2: Test Runner & Grading**

## OpenAPI Documentation

When running, API documentation is available at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
