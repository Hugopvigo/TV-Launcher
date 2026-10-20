@echo off
REM Build del launcher: Gradle 8.14.3 + JDK 21 + SDK de Android en Windows.
setlocal
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21
set ANDROID_HOME=C:\Users\Hugop\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=C:\Users\Hugop\AppData\Local\Android\Sdk
set GRADLE_USER_HOME=C:\Users\Hugop\.gradle
cd /d C:\Users\Hugop\Dev\firestick-debloat\launcher
call C:\Users\Hugop\.gradle\wrapper\dists\gradle-8.14.3-bin\cv11ve7ro1n3o1j4so8xd9n66\gradle-8.14.3\bin\gradle.bat %*
endlocal
