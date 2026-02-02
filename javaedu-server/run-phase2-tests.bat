@echo off
setlocal EnableDelayedExpansion

REM Find Java 17
for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do set "JAVA_HOME=%%i"

if not defined JAVA_HOME (
    echo Java 17 not found
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using JAVA_HOME: %JAVA_HOME%
java -version

echo.
echo Running Phase 2 tests...
echo.

call mvn test -Dtest=TestRunnerServiceTest,GradingServiceTest,SubmissionControllerTest,SubmissionServiceTest

echo.
echo Done.
