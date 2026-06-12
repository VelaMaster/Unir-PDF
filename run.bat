@echo off
REM Lanza la aplicación Unir PDF.
REM Requiere haber compilado previamente con: mvn clean package
set JAR=%~dp0target\UnirPDF.jar
if not exist "%JAR%" (
    echo No se encontro %JAR%
    echo Compila el proyecto primero con: mvn clean package
    pause
    exit /b 1
)
start javaw -jar "%JAR%"
