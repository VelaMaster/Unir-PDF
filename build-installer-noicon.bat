@echo off
REM Igual que build-installer.bat pero sin icono personalizado.
REM Usa este si todavia no tienes assets\icon.ico.

setlocal enabledelayedexpansion
pushd "%~dp0"

echo [1/3] Compilando con Maven...
call mvn -q clean package
if errorlevel 1 ( pause & popd & exit /b 1 )

echo [2/3] Preparando carpeta de entrada...
if exist build\input rmdir /s /q build\input
mkdir build\input
copy /Y target\UnirPDF.jar build\input\UnirPDF.jar >nul
if exist dist rmdir /s /q dist
mkdir dist

echo [3/3] Generando instalador con jpackage...
jpackage ^
    --type exe ^
    --name "Unir PDF" ^
    --app-version 1.0.0 ^
    --vendor "Diego Barrios" ^
    --description "Aplicacion para unir PDFs de multiples carpetas" ^
    --input build\input ^
    --main-jar UnirPDF.jar ^
    --main-class com.unirpdf.app.Main ^
    --dest dist ^
    --win-dir-chooser ^
    --win-shortcut ^
    --win-menu ^
    --win-menu-group "Unir PDF"

if errorlevel 1 ( pause & popd & exit /b 1 )

echo.
echo Instalador generado en: %CD%\dist
pause
popd
endlocal
