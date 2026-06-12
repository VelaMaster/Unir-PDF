@echo off
REM ==========================================================================
REM  Genera un instalador .exe de Unir PDF con la JRE embebida.
REM  El usuario final NO necesita tener Java instalado.
REM
REM  Requisitos para EJECUTAR ESTE SCRIPT (solo tu, el desarrollador):
REM    - JDK 17 o superior (incluye jpackage)
REM    - Maven en el PATH
REM    - WiX Toolset 3.x  (para crear instalador .msi/.exe en Windows)
REM      Descarga: https://github.com/wixtoolset/wix3/releases
REM
REM  Resultado:
REM    dist\UnirPDF-1.0.0.exe   <-- entregable para tus usuarios
REM ==========================================================================

setlocal enabledelayedexpansion
pushd "%~dp0"

echo.
echo [1/3] Compilando con Maven...
call mvn -q clean package
if errorlevel 1 (
    echo ERROR: Falló la compilación con Maven.
    pause
    popd
    exit /b 1
)

echo.
echo [2/3] Preparando carpeta de entrada...
if exist build\input rmdir /s /q build\input
mkdir build\input
copy /Y target\UnirPDF.jar build\input\UnirPDF.jar >nul

if exist dist rmdir /s /q dist
mkdir dist

echo.
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
    --win-menu-group "Unir PDF" ^
    --win-shortcut-prompt ^
    --icon assets\icon.ico

if errorlevel 1 (
    echo.
    echo ERROR: jpackage fallo. Verifica que tengas:
    echo   - JDK 17+ en el PATH
    echo   - WiX Toolset instalado (candle.exe y light.exe accesibles)
    pause
    popd
    exit /b 1
)

echo.
echo ===========================================================
echo  Instalador generado en: %CD%\dist
echo  Distribuye el .exe a tus usuarios. No necesitan Java.
echo ===========================================================
echo.
pause
popd
endlocal
