package com.unirpdf.ui;

import com.unirpdf.model.PdfFile;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo para la lista de PDFs en el orden de unión.
 * Permite reordenar elementos (subir/bajar/drag&drop).
 */
public class SelectedListModel extends DefaultListModel<PdfFile> {

    public List<PdfFile> getAll() {
        List<PdfFile> list = new ArrayList<>();
        for (int i = 0; i < size(); i++) list.add(get(i));
        return list;
    }

    public void moveUp(int index) {
        if (index <= 0 || index >= size()) return;
        PdfFile item = get(index);
        remove(index);
        add(index - 1, item);
    }

    public void moveDown(int index) {
        if (index < 0 || index >= size() - 1) return;
        PdfFile item = get(index);
        remove(index);
        add(index + 1, item);
    }

    public void moveItem(int from, int to) {
        if (from < 0 || from >= size() || to < 0 || to > size() || from == to) return;
        PdfFile item = get(from);
        remove(from);
        if (to > from) to--;
        add(to, item);
    }

    public void syncWith(List<PdfFile> selected) {
        // Eliminar los que ya no están seleccionados
        List<PdfFile> current = getAll();
        for (PdfFile p : current) {
            if (!selected.contains(p)) removeElement(p);
        }
        // Añadir los nuevos al final manteniendo el orden previo
        for (PdfFile p : selected) {
            if (!contains(p)) addElement(p);
        }
    }
}
