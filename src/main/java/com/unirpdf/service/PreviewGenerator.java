package com.unirpdf.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Genera vistas previas (BufferedImage) de las páginas de un PDF.
 * Soporta rotación arbitraria del usuario (0/90/180/270).
 */
public class PreviewGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewGenerator.class);

    /** DPI razonable para una previsualización legible y rápida. */
    private static final float DPI = 90f;

    /**
     * Devuelve el número de páginas del PDF, o 0 si no se puede leer.
     */
    public int getPageCount(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) return 0;
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            return doc.getNumberOfPages();
        } catch (IOException ex) {
            LOGGER.warn("No se pudo obtener el número de páginas de {}: {}", pdfFile, ex.getMessage());
            return 0;
        }
    }

    /**
     * Renderiza una página específica del PDF.
     *
     * @param pdfFile    archivo PDF
     * @param pageIndex  índice 0-based de la página
     * @param extraRotation rotación adicional del usuario (0,90,180,270)
     * @return imagen renderizada (ya rotada) o null si no se puede
     */
    public BufferedImage renderPage(File pdfFile, int pageIndex, int extraRotation) {
        if (pdfFile == null || !pdfFile.exists()) return null;
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            int n = doc.getNumberOfPages();
            if (n == 0 || pageIndex < 0 || pageIndex >= n) return null;
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage img = renderer.renderImageWithDPI(pageIndex, DPI);
            int rot = ((extraRotation % 360) + 360) % 360;
            if (rot == 0) return img;
            return rotateImage(img, rot);
        } catch (IOException ex) {
            LOGGER.warn("No se pudo renderizar página {} de {}: {}",
                    pageIndex, pdfFile, ex.getMessage());
            return null;
        }
    }

    /** Compatibilidad con código previo. */
    public BufferedImage renderFirstPage(File pdfFile) {
        return renderPage(pdfFile, 0, 0);
    }

    private BufferedImage rotateImage(BufferedImage src, int degrees) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst;
        AffineTransform tx = new AffineTransform();
        switch (degrees) {
            case 90:
                dst = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
                tx.translate(h, 0);
                tx.rotate(Math.toRadians(90));
                break;
            case 180:
                dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                tx.translate(w, h);
                tx.rotate(Math.toRadians(180));
                break;
            case 270:
                dst = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
                tx.translate(0, w);
                tx.rotate(Math.toRadians(270));
                break;
            default:
                return src;
        }
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, tx, null);
        g.dispose();
        return dst;
    }
}
