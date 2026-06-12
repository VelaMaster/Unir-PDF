package com.unirpdf.util;

import javax.swing.*;
import java.awt.*;

/** Utilidades comunes para la UI. */
public final class UiUtils {

    private UiUtils() {}

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /** Aplica padding (insets) uniforme a un componente. */
    public static JComponent withPadding(JComponent c, int padding) {
        c.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return c;
    }
}
