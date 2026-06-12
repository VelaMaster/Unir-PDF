package com.unirpdf.app;

import com.formdev.flatlaf.FlatLightLaf;
import com.unirpdf.ui.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Punto de entrada de la aplicación.
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Captura cualquier excepción no manejada en el EDT
        Thread.setDefaultUncaughtExceptionHandler((t, e) ->
                LOGGER.error("Excepción no manejada en {}: {}", t.getName(), e.getMessage(), e));

        // Look and feel moderno
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            // Pequeños ajustes visuales
            UIManager.put("Component.arc", 8);
            UIManager.put("Button.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        } catch (Exception ex) {
            LOGGER.warn("No se pudo aplicar FlatLaf, usando L&F por defecto", ex);
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            LOGGER.info("Aplicación iniciada.");
        });
    }
}
