package com.unirpdf.ui;

import com.unirpdf.model.PdfFile;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Transfer handler que habilita arrastrar y soltar para reordenar elementos
 * dentro de un JList que use {@link SelectedListModel}.
 */
public class ListReorderHandler extends TransferHandler {

    private static final DataFlavor LOCAL_FLAVOR =
            new DataFlavor(Integer.class, "selected-list-index");

    private int sourceIndex = -1;

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JList<?> list = (JList<?>) c;
        sourceIndex = list.getSelectedIndex();
        return new Transferable() {
            @Override public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{LOCAL_FLAVOR};
            }
            @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
                return LOCAL_FLAVOR.equals(flavor);
            }
            @Override public Object getTransferData(DataFlavor flavor)
                    throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
                return sourceIndex;
            }
        };
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDrop() && support.isDataFlavorSupported(LOCAL_FLAVOR);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;
        try {
            JList<PdfFile> list = (JList<PdfFile>) support.getComponent();
            SelectedListModel model = (SelectedListModel) list.getModel();

            int from = (Integer) support.getTransferable().getTransferData(LOCAL_FLAVOR);
            JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
            int to = dl.getIndex();

            model.moveItem(from, to);
            // Selección al nuevo índice
            int newIndex = (to > from) ? to - 1 : to;
            list.setSelectedIndex(Math.min(newIndex, model.size() - 1));
            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            return false;
        }
    }
}
