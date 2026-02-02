@echo off
setlocal EnableDelayedExpansion

echo ========================================
echo JavaEdu Server - Setup Verification
echo ========================================
echo.

REM Set JAVA_HOME to Java 17 if available (adjust path as needed)
if exist "C:\Program Files\Eclipse Adoptium\jdk-17*" (
    for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do set "JAVA_HOME=%%i"
    set "PATH=!JAVA_HOME!\bin;!PATH!"
    echo Using Java 17 from: !JAVA_HOME!
    echo.
)

echo Step 1: Checking Java version...
java -version 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java not found!
    echo.
    echo To install Java 17:
    echo   1. Download from: https://adoptium.net/
    echo   2. Run the installer
    echo   3. Open a NEW command prompt and try again
    echo.
    echo Or with Chocolatey: choco install temurin17
    goto :end
)
echo [OK] Java found
echo.

echo Step 2: Checking Maven...
call mvn -version 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven not found!
    echo.
    echo To install Maven:
    echo   1. Download from: https://maven.apache.org/download.cgi
    echo   2. Extract to C:\Program Files\Apache\maven
    echo   3. Add MAVEN_HOME environment variable pointing to that folder
    echo   4. Add %%MAVEN_HOME%%\bin to your PATH
    echo   5. Open a NEW command prompt and try again
    echo.
    echo Or with Chocolatey: choco install maven
    goto :end
)
echo [OK] Maven found
echo.

echo Step 3: Compiling project...
call mvn clean compile -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed. Check the errors above.
    goto :end
)
echo [OK] Project compiles successfully
echo.

echo Step 4: Running tests...
call mvn test
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [WARNING] Some tests failed. Check output above.
) else (
    echo [OK] All tests passed
)
echo.

echo ========================================
echo Setup verification complete!
echo.
echo Next steps:
echo   1. Run: run-dev.bat
echo   2. Open: http://localhost:8080/api/health
echo ========================================

:end
echo.
pause
