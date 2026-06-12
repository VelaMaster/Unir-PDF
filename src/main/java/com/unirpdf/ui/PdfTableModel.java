package com.unirpdf.ui;

import com.unirpdf.model.PdfFile;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de tabla para los PDFs encontrados.
 * Columnas: [✓] | Nombre | Carpeta | Ruta completa | Tamaño | Fecha modificación
 */
public class PdfTableModel extends AbstractTableModel {

    public static final int COL_SELECTED = 0;
    public static final int COL_NAME = 1;
    public static final int COL_FOLDER = 2;
    public static final int COL_PATH = 3;
    public static final int COL_SIZE = 4;
    public static final int COL_DATE = 5;

    private static final String[] COLS = {
            "✓", "Nombre", "Carpeta", "Ruta", "Tamaño", "Fecha modificación"
    };

    private static final Class<?>[] TYPES = {
            Boolean.class, String.class, String.class, String.class, String.class, String.class
    };

    private final List<PdfFile> data = new ArrayList<>();
    private SelectionListener selectionListener;

    public interface SelectionListener {
        void onSelectionChanged();
    }

    public void setSelectionListener(SelectionListener l) {
        this.selectionListener = l;
    }

    public void setData(List<PdfFile> pdfs) {
        data.clear();
        if (pdfs != null) data.addAll(pdfs);
        fireTableDataChanged();
        notifySelection();
    }

    public PdfFile getAt(int row) {
        return data.get(row);
    }

    public List<PdfFile> getAll() {
        return new ArrayList<>(data);
    }

    public List<PdfFile> getSelected() {
        List<PdfFile> sel = new ArrayList<>();
        for (PdfFile p : data) if (p.isSelected()) sel.add(p);
        return sel;
    }

    public void selectAll(boolean selected) {
        for (PdfFile p : data) p.setSelected(selected);
        fireTableDataChanged();
        notifySelection();
    }

    public void invertSelection() {
        for (PdfFile p : data) p.setSelected(!p.isSelected());
        fireTableDataChanged();
        notifySelection();
    }

    public int countSelected() {
        int n = 0;
        for (PdfFile p : data) if (p.isSelected()) n++;
        return n;
    }

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return COLS.length; }
    @Override public String getColumnName(int c) { return COLS[c]; }
    @Override public Class<?> getColumnClass(int c) { return TYPES[c]; }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == COL_SELECTED;
    }

    @Override
    public Object getValueAt(int row, int col) {
        PdfFile p = data.get(row);
        switch (col) {
            case COL_SELECTED: return p.isSelected();
            case COL_NAME:     return p.getName();
            case COL_FOLDER:   return p.getParentFolder();
            case COL_PATH:     return p.getAbsolutePath();
            case COL_SIZE:     return p.getSizeFormatted();
            case COL_DATE:     return p.getLastModifiedFormatted();
            default:           return null;
        }
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (col == COL_SELECTED && value instanceof Boolean) {
            data.get(row).setSelected((Boolean) value);
            fireTableCellUpdated(row, col);
            notifySelection();
        }
    }

    private void notifySelection() {
        if (selectionListener != null) selectionListener.onSelectionChanged();
    }
}
