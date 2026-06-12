package com.unirpdf.service;

import com.unirpdf.model.PdfFile;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Une varios PDFs en un único PDF respetando el orden recibido.
 * Aplica las rotaciones por página configuradas por el usuario.
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

                Map<Integer, Integer> rotations = src.getPageRotations();
                try (PDDocument doc = Loader.loadPDF(srcFile)) {
                    // Aplicar rotaciones del usuario antes de combinar.
                    if (rotations != null && !rotations.isEmpty()) {
                        int n = doc.getNumberOfPages();
                        for (Map.Entry<Integer, Integer> e : rotations.entrySet()) {
                            int idx = e.getKey();
                            int extra = e.getValue();
                            if (idx < 0 || idx >= n || extra == 0) continue;
                            PDPage page = doc.getPage(idx);
                            int current = page.getRotation();
                            int combined = ((current + extra) % 360 + 360) % 360;
                            page.setRotation(combined);
                        }
                    }
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
