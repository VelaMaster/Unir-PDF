# Unir PDF

Aplicación de escritorio para Windows 10/11 que permite unir archivos PDF ubicados en múltiples carpetas y subcarpetas. El usuario selecciona manualmente qué PDFs unir y en qué orden — no se realiza OCR ni identificación automática.

## Características

- Escaneo recursivo de una carpeta raíz buscando todos los `.pdf`.
- Tabla con casilla de selección, nombre, carpeta, ruta, tamaño y fecha.
- Filtro/búsqueda por nombre, carpeta o ruta libre.
- Vista previa de la primera página del PDF seleccionado.
- Reordenado de la lista de unión por **arrastrar y soltar** o botones **subir / bajar**.
- Contador de PDFs encontrados y seleccionados.
- Botones **Seleccionar todo**, **Deseleccionar todo**, **Invertir selección**.
- Barra de progreso durante la unión.
- Mensajes de éxito o error.
- Recuerda la última carpeta escaneada y la última ubicación/nombre de salida.
- Registro de errores y eventos en archivo log.
- Interfaz moderna con FlatLaf.

## Distribución para usuarios finales (recomendado)

El proyecto está pensado para que **los usuarios finales NO tengan que instalar Java**. Tú generas un instalador `.exe` con la JRE embebida y se lo das. Ellos:

1. Hacen doble click sobre `Unir PDF-1.0.0.exe`.
2. Siguen el asistente "Siguiente, Siguiente, Finalizar".
3. Les aparece un acceso directo en el escritorio y en el menú Inicio.
4. Doble click sobre el icono → se abre la app. Listo.

### Requisitos para generar el instalador (solo tú, una vez)

- **JDK 17 o superior** (incluye `jpackage`). Descarga: https://adoptium.net/
- **Maven 3.6+**
- **WiX Toolset 3.x** para que `jpackage` pueda crear el `.exe`. Descarga: https://github.com/wixtoolset/wix3/releases — instala el `.exe` de WiX y reinicia la terminal.

### Generar el instalador

Desde la raíz del proyecto, en una terminal de Windows:

```bat
build-installer-noicon.bat
```

(O `build-installer.bat` si ya tienes `assets\icon.ico`).

Resultado: `dist\Unir PDF-1.0.0.exe`. Ese archivo es lo que distribuyes.

---

## Alternativa: JAR ejecutable (para usuarios con Java instalado)

### Requisitos para compilar

- **JDK 11** o superior
- **Maven 3.6+**

### Compilar

```bash
mvn clean package
```

Genera un **fat JAR ejecutable** en `target/UnirPDF.jar` con todas las dependencias incluidas.

### Ejecutar

```bash
java -jar target/UnirPDF.jar
```

En Windows, doble click sobre `UnirPDF.jar` si Java está asociado a `.jar`. También puedes usar `run.bat`.

### Compatibilidad

- Bytecode generado para **Java 11**, así que funciona con cualquier JRE 11, 17, 21 o superior — versiones antiguas y nuevas.

## Archivos de configuración y logs

Se crean automáticamente en `%USERPROFILE%\.unirpdf\`:

- `config.properties` — última carpeta usada, última ubicación y nombre de salida.
- `logs\unirpdf.log` — log diario rotado (máx. 14 días, 50 MB).

## Estructura del proyecto

```
src/main/java/com/unirpdf/
├── app/         → punto de entrada (Main)
├── model/       → modelos (PdfFile)
├── service/     → lógica de negocio
│   ├── PdfScanner       — búsqueda recursiva
│   ├── PdfMerger        — unión de PDFs con progreso
│   ├── PreviewGenerator — render de la primera página
│   └── ConfigManager    — preferencias persistentes
├── ui/          → interfaz Swing
│   ├── MainFrame
│   ├── PdfTableModel
│   ├── SelectedListModel
│   ├── ListReorderHandler (drag & drop)
│   └── PreviewPanel
└── util/        → utilidades
    └── UiUtils
```

Arquitectura por capas: **UI ↔ Servicios ↔ Modelo**. Las operaciones largas (escaneo, render, unión) se ejecutan en `SwingWorker` para no bloquear la interfaz.

## Dependencias

| Librería       | Uso                                  |
|----------------|--------------------------------------|
| Apache PDFBox  | Lectura, unión y render de PDFs      |
| FlatLaf        | Look and feel moderno para Swing     |
| SLF4J + Logback| Registro de eventos y errores        |

## Uso rápido

1. Pulsa **Seleccionar carpeta raíz** y elige la carpeta de expedientes.
2. Espera a que termine el escaneo (barra de progreso indeterminada).
3. Marca las casillas de los PDFs que quieras incluir. Usa el filtro para acotar.
4. Pulsa una fila para ver la vista previa.
5. Reordena la lista de la derecha con arrastrar y soltar o los botones ▲ / ▼.
6. Pulsa **Unir PDFs seleccionados**, elige nombre/ubicación y espera.
# Unir-PDF
