package com.unirpdf.service;

import com.unirpdf.model.PdfFile;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Une varios PDFs en un único PDF respetando el orden recibido.
 * Reporta progreso (archivo actual / total).
 */
public class PdfMerger {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfMerger.class);

    /**
     * Une la lista de PDFs en {@code output}.
     *
     * @param sources    lista ordenada de PDFs a unir
     * @param output     archivo PDF de salida
     * @param onProgress callback (procesados, total) — puede ser null
     */
    public void merge(List<PdfFile> sources, File output,
                      BiConsumer<Integer, Integer> onProgress) throws IOException {

        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("No hay PDFs para unir.");
        }
        if (output == null) {
            throw new IllegalArgumentException("El archivo de salida es null.");
        }

        File parent = output.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de destino: " + parent);
        }

        LOGGER.info("Iniciando unión de {} PDFs en {}", sources.size(), output.getAbsolutePath());

        // Usamos PDDocument destino y añadimos páginas para reportar progreso por archivo
        try (PDDocument target = new PDDocument()) {
            PDFMergerUtility util = new PDFMergerUtility();
            int total = sources.size();
            int i = 0;

            for (PdfFile src : sources) {
                i++;
                File srcFile = src.getFile();
                if (!srcFile.exists()) {
                    LOGGER.warn("Archivo no encontrado, se omite: {}", srcFile);
                    if (onProgress != null) onProgress.accept(i, total);
                    continue;
                }

                try (PDDocument doc = Loader.loadPDF(srcFile)) {
                    util.appendDocument(target, doc);
                } catch (IOException ex) {
                    LOGGER.error("Error añadiendo PDF {}: {}", srcFile, ex.getMessage());
                    throw new IOException("Error procesando: " + srcFile.getName() + " — " + ex.getMessage(), ex);
                }

                if (onProgress != null) onProgress.accept(i, total);
            }

            target.save(output);
            LOGGER.info("PDF generado correctamente: {}", output.getAbsolutePath());
        }
    }
}
