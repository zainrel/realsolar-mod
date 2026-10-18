@echo off
chcp 65001 >nul
echo ========================================================
echo   Сборка мода Realistic Solar Cycle 1.1.0 для MC 26.3
echo ========================================================
echo.
if exist gradlew.bat (
    call gradlew.bat build
) else (
    where gradle >nul 2>nul
    if %errorlevel%==0 (
        gradle build
    ) else (
        echo [!] Файл gradlew.bat не найден.
        echo Откройте эту папку в бесплатной программе IntelliJ IDEA
        echo или скачайте файлы gradlew с https://fabricmc.net/develop/template/
        pause
        exit /b 1
    )
)
echo.
echo ========================================================
echo   ГОТОВО! Ваш готовый .jar файл лежит в папке:
echo   build\libs\realsolar-1.1.0+26.3.jar
echo ========================================================
pause
