package com.unirpdf.ui;

import com.unirpdf.model.PdfFile;
import com.unirpdf.service.PreviewGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Panel que muestra la vista previa de la primera página del PDF.
 * Renderiza en segundo plano para no congelar la UI.
 */
public class PreviewPanel extends JPanel {

    private final PreviewGenerator generator = new PreviewGenerator();
    private final JLabel imageLabel = new JLabel("Selecciona un PDF para vista previa",
            SwingConstants.CENTER);
    private final AtomicReference<PdfFile> currentRequest = new AtomicReference<>();
    private SwingWorker<BufferedImage, Void> worker;

    public PreviewPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Vista previa"));
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setForeground(Color.GRAY);
        JScrollPane scroll = new JScrollPane(imageLabel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        setPreferredSize(new Dimension(360, 500));
    }

    public void showPreview(PdfFile pdf) {
        currentRequest.set(pdf);
        if (pdf == null) {
            imageLabel.setIcon(null);
            imageLabel.setText("Selecciona un PDF para vista previa");
            return;
        }

        imageLabel.setIcon(null);
        imageLabel.setText("Generando vista previa...");

        if (worker != null && !worker.isDone()) worker.cancel(true);

        worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() {
                return generator.renderFirstPage(pdf.getFile());
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                if (currentRequest.get() != pdf) return; // selección cambió
                try {
                    BufferedImage img = get();
                    if (img == null) {
                        imageLabel.setIcon(null);
                        imageLabel.setText("No se pudo generar la vista previa");
                        return;
                    }
                    // Ajustar al ancho disponible
                    int targetWidth = Math.max(280, getWidth() - 40);
                    if (img.getWidth() > targetWidth) {
                        double ratio = targetWidth / (double) img.getWidth();
                        int h = (int) (img.getHeight() * ratio);
                        Image scaled = img.getScaledInstance(targetWidth, h, Image.SCALE_SMOOTH);
                        imageLabel.setIcon(new ImageIcon(scaled));
                    } else {
                        imageLabel.setIcon(new ImageIcon(img));
                    }
                    imageLabel.setText(null);
                } catch (Exception ex) {
                    imageLabel.setIcon(null);
                    imageLabel.setText("Error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
