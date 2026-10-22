@echo off
setlocal
cd /d "%~dp0"

rem Gradle 8.8 cannot run on Java 24, so prefer a JDK 17-21 install.
set "JAVA_HOME="
for %%D in (
	"C:\Program Files\Java\jdk-21"
	"C:\Program Files\Java\jdk-17"
	"C:\Program Files\Eclipse Adoptium\jdk-21*"
	"C:\Program Files\Eclipse Adoptium\jdk-17*"
) do (
	if not defined JAVA_HOME if exist "%%~fD\bin\java.exe" set "JAVA_HOME=%%~fD"
)
if not defined JAVA_HOME (
	echo ERROR: Could not find JDK 17 or 21. Install one or set JAVA_HOME.
	exit /b 1
)

echo Using JAVA_HOME=%JAVA_HOME%
echo Building viruzosbot plugin jar...
call gradlew.bat :client:viruzosbotPluginJar --console=plain
if errorlevel 1 (
	echo ERROR: Build failed.
	if /i not "%~1"=="/nopause" pause
	exit /b 1
)

set "DEST=%USERPROFILE%\.runelite\microbot-plugins"
if not exist "%DEST%" mkdir "%DEST%"
copy /Y "runelite-client\build\libs\viruzosbot.jar" "%DEST%\viruzosbot.jar" >nul
if errorlevel 1 (
	echo ERROR: Could not copy jar to %DEST%
	if /i not "%~1"=="/nopause" pause
	exit /b 1
)

echo Exported to %DEST%\viruzosbot.jar
if /i not "%~1"=="/nopause" pause
endlocal
