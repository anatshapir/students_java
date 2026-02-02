@echo off
echo ========================================
echo JavaEdu Server - Development Mode
echo ========================================
echo.
echo Starting server with H2 in-memory database...
echo.
echo Once started, you can:
echo   - Health check: http://localhost:8080/api/health
echo   - H2 Console:   http://localhost:8080/h2-console
echo   - Swagger UI:   http://localhost:8080/swagger-ui.html
echo.
echo Press Ctrl+C to stop the server.
echo ========================================
echo.

call mvn spring-boot:run -Dspring-boot.run.profiles=dev
pause
