@echo off
REM ==========================================
REM Set Java 17 for this project
REM ==========================================
REM
REM Edit the JAVA_HOME path below to match your Java 17 installation.
REM Common locations:
REM   - Chocolatey temurin17: C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot
REM   - Oracle JDK 17: C:\Program Files\Java\jdk-17
REM   - Manual install: wherever you extracted it
REM

REM Auto-detect Eclipse Adoptium (Temurin) Java 17
for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do set "JAVA_HOME=%%i"

REM If not found, try Oracle JDK location
if not defined JAVA_HOME (
    if exist "C:\Program Files\Java\jdk-17" set "JAVA_HOME=C:\Program Files\Java\jdk-17"
)

REM If still not found, show error
if not defined JAVA_HOME (
    echo ERROR: Java 17 not found!
    echo.
    echo Please install Java 17:
    echo   choco install temurin17
    echo.
    echo Or edit this file and set JAVA_HOME manually.
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo JAVA_HOME set to: %JAVA_HOME%
java -version
