package com.unirpdf.service;

import com.unirpdf.model.PdfFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Recorre recursivamente una carpeta raíz buscando archivos PDF.
 */
public class PdfScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfScanner.class);

    /**
     * Escanea recursivamente la carpeta raíz y devuelve los PDFs encontrados.
     *
     * @param root      carpeta raíz
     * @param onProgress callback opcional con cada archivo encontrado (puede ser null)
     */
    public List<PdfFile> scan(Path root, Consumer<PdfFile> onProgress) throws IOException {
        if (root == null || !Files.isDirectory(root)) {
            throw new IllegalArgumentException("La carpeta raíz no es válida: " + root);
        }

        LOGGER.info("Iniciando escaneo de carpeta: {}", root);
        List<PdfFile> result = new ArrayList<>();

        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile() && isPdf(file)) {
                    PdfFile pdf = new PdfFile(file.toFile());
                    result.add(pdf);
                    if (onProgress != null) onProgress.accept(pdf);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOGGER.warn("No se pudo acceder al archivo: {} ({})", file, exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });

        LOGGER.info("Escaneo completado. PDFs encontrados: {}", result.size());
        return result;
    }

    private static boolean isPdf(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".pdf");
    }
}
