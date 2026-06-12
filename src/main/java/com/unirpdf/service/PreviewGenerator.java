package com.unirpdf.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Genera una vista previa (BufferedImage) de la primera página de un PDF.
 */
public class PreviewGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewGenerator.class);

    /** DPI razonable para una previsualización legible y rápida. */
    private static final float DPI = 90f;

    /**
     * Renderiza la primera página del PDF dado.
     *
     * @return imagen renderizada o null si no se puede generar
     */
    public BufferedImage renderFirstPage(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) return null;
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            if (doc.getNumberOfPages() == 0) return null;
            PDFRenderer renderer = new PDFRenderer(doc);
            return renderer.renderImageWithDPI(0, DPI);
        } catch (IOException ex) {
            LOGGER.warn("No se pudo renderizar la vista previa de {}: {}", pdfFile, ex.getMessage());
            return null;
        }
    }
}
