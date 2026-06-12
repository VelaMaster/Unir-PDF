package com.unirpdf.ui;

import com.unirpdf.model.PdfFile;
import com.unirpdf.service.PreviewGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Panel que muestra la vista previa completa del PDF seleccionado, página
 * por página, tal y como aparecerá en el PDF unido. Permite rotar páginas
 * sueltas con ↺ ↻; la rotación queda guardada en el {@link PdfFile} y se
 * aplica al unir.
 */
public class PreviewPanel extends JPanel {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewPanel.class);

    private final PreviewGenerator generator = new PreviewGenerator();
    private final JPanel pagesPanel = new JPanel();
    private final JLabel emptyLabel = new JLabel("Selecciona un PDF para vista previa",
            SwingConstants.CENTER);
    private final JLabel headerLabel = new JLabel(" ");
    private final JScrollPane scroll;
    private final AtomicReference<PdfFile> currentRequest = new AtomicReference<>();
    private SwingWorker<?, ?> worker;

    public PreviewPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Vista previa (así quedará el PDF unido)"));

        pagesPanel.setLayout(new BoxLayout(pagesPanel, BoxLayout.Y_AXIS));
        pagesPanel.setBackground(new Color(0xEEEEEE));

        emptyLabel.setVerticalAlignment(SwingConstants.CENTER);
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setForeground(Color.GRAY);

        scroll = new JScrollPane(emptyLabel);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setBorder(null);

        headerLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        headerLabel.setForeground(new Color(0x555555));

        add(headerLabel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        setPreferredSize(new Dimension(420, 520));
    }

    public void showPreview(PdfFile pdf) {
        currentRequest.set(pdf);
        if (pdf == null) {
            headerLabel.setText(" ");
            scroll.setViewportView(emptyLabel);
            return;
        }

        headerLabel.setText("Cargando: " + pdf.getName());
        emptyLabel.setText("Generando vista previa...");
        scroll.setViewportView(emptyLabel);

        if (worker != null && !worker.isDone()) worker.cancel(true);

        worker = new SwingWorker<List<BufferedImage>, Void>() {
            int totalPages = 0;

            @Override
            protected List<BufferedImage> doInBackground() {
                totalPages = generator.getPageCount(pdf.getFile());
                List<BufferedImage> imgs = new ArrayList<>();
                for (int i = 0; i < totalPages; i++) {
                    if (isCancelled()) return imgs;
                    int rot = pdf.getPageRotation(i);
                    BufferedImage img = generator.renderPage(pdf.getFile(), i, rot);
                    imgs.add(img);
                }
                return imgs;
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                if (currentRequest.get() != pdf) return;
                try {
                    List<BufferedImage> imgs = get();
                    if (imgs.isEmpty()) {
                        emptyLabel.setText("No se pudo generar la vista previa");
                        scroll.setViewportView(emptyLabel);
                        return;
                    }
                    rebuildPagesPanel(pdf, imgs);
                    headerLabel.setText(pdf.getName() + "  ·  " + imgs.size()
                            + (imgs.size() == 1 ? " página" : " páginas"));
                    scroll.setViewportView(pagesPanel);
                    scroll.getVerticalScrollBar().setValue(0);
                } catch (Exception ex) {
                    LOGGER.warn("Error mostrando vista previa", ex);
                    emptyLabel.setText("Error: " + ex.getMessage());
                    scroll.setViewportView(emptyLabel);
                }
            }
        };
        worker.execute();
    }

    private void rebuildPagesPanel(PdfFile pdf, List<BufferedImage> imgs) {
        pagesPanel.removeAll();
        int targetWidth = Math.max(300, getWidth() - 60);
        for (int i = 0; i < imgs.size(); i++) {
            pagesPanel.add(buildPageCard(pdf, i, imgs.get(i), targetWidth));
            pagesPanel.add(Box.createVerticalStrut(10));
        }
        pagesPanel.revalidate();
        pagesPanel.repaint();
    }

    private JComponent buildPageCard(PdfFile pdf, int pageIndex, BufferedImage img, int maxWidth) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCCCCCC)),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Encabezado: nº de página + botones de rotación
        JLabel pageLabel = new JLabel("Página " + (pageIndex + 1));
        pageLabel.setFont(pageLabel.getFont().deriveFont(Font.BOLD));

        JButton rotLeft = new JButton("↺");
        JButton rotRight = new JButton("↻");
        rotLeft.setToolTipText("Rotar 90° izquierda");
        rotRight.setToolTipText("Rotar 90° derecha");
        rotLeft.setMargin(new Insets(2, 6, 2, 6));
        rotRight.setMargin(new Insets(2, 6, 2, 6));

        int rot = pdf.getPageRotation(pageIndex);
        JLabel rotLabel = new JLabel(rot == 0 ? "" : (rot + "°"));
        rotLabel.setForeground(new Color(0x1976D2));

        rotLeft.addActionListener(e -> {
            pdf.rotatePageCounterClockwise(pageIndex);
            refreshSinglePage(pdf, pageIndex);
        });
        rotRight.addActionListener(e -> {
            pdf.rotatePageClockwise(pageIndex);
            refreshSinglePage(pdf, pageIndex);
        });

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        headerLeft.setOpaque(false);
        headerLeft.add(pageLabel);
        headerLeft.add(rotLabel);
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        headerRight.setOpaque(false);
        headerRight.add(rotLeft);
        headerRight.add(rotRight);
        header.add(headerLeft, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);

        // Imagen escalada
        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        if (img == null) {
            imageLabel.setText("(no se pudo renderizar la página)");
            imageLabel.setForeground(Color.GRAY);
        } else {
            Image scaled = scaleToWidth(img, maxWidth);
            imageLabel.setIcon(new ImageIcon(scaled));
        }

        card.add(header, BorderLayout.NORTH);
        card.add(imageLabel, BorderLayout.CENTER);
        return card;
    }

    /** Re-renderiza solo una página y la reemplaza en el panel sin recargar el documento entero. */
    private void refreshSinglePage(PdfFile pdf, int pageIndex) {
        if (currentRequest.get() != pdf) return;
        int targetWidth = Math.max(300, getWidth() - 60);
        SwingWorker<BufferedImage, Void> w = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() {
                return generator.renderPage(pdf.getFile(), pageIndex, pdf.getPageRotation(pageIndex));
            }
            @Override
            protected void done() {
                if (currentRequest.get() != pdf) return;
                try {
                    BufferedImage img = get();
                    // índice en pagesPanel: cada página ocupa 2 hijos (card + strut)
                    int childIdx = pageIndex * 2;
                    if (childIdx >= pagesPanel.getComponentCount()) return;
                    Component existing = pagesPanel.getComponent(childIdx);
                    Component newCard = buildPageCard(pdf, pageIndex, img, targetWidth);
                    pagesPanel.remove(childIdx);
                    pagesPanel.add(newCard, childIdx);
                    pagesPanel.revalidate();
                    pagesPanel.repaint();
                } catch (Exception ex) {
                    LOGGER.warn("Error refrescando página {}: {}", pageIndex, ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private Image scaleToWidth(BufferedImage img, int targetWidth) {
        if (img.getWidth() <= targetWidth) return img;
        double ratio = targetWidth / (double) img.getWidth();
        int h = (int) (img.getHeight() * ratio);
        return img.getScaledInstance(targetWidth, h, Image.SCALE_SMOOTH);
    }
}
