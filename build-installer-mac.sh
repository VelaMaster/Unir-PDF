#!/bin/bash
# Genera un instalador .dmg de Unir PDF para macOS con la JRE embebida.
# Requiere: JDK 17+ y Maven en el PATH.
# Resultado: dist/Unir PDF-1.0.0.dmg

set -e
cd "$(dirname "$0")"

echo "[1/3] Compilando con Maven..."
mvn -q clean package

echo "[2/3] Preparando carpeta de entrada..."
rm -rf build/input dist
mkdir -p build/input dist
cp target/UnirPDF.jar build/input/UnirPDF.jar

echo "[3/3] Generando instalador con jpackage..."
jpackage \
    --type dmg \
    --name "Unir PDF" \
    --app-version 1.0.0 \
    --vendor "Diego Barrios" \
    --description "Aplicacion para unir PDFs de multiples carpetas" \
    --input build/input \
    --main-jar UnirPDF.jar \
    --main-class com.unirpdf.app.Main \
    --dest dist

echo ""
echo "Instalador generado en: $(pwd)/dist"
