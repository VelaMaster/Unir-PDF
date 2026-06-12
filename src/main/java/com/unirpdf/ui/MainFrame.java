package com.unirpdf.ui;

import com.unirpdf.model.PdfFile;
import com.unirpdf.service.ConfigManager;
import com.unirpdf.service.PdfMerger;
import com.unirpdf.service.PdfScanner;
import com.unirpdf.util.UiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Ventana principal de la aplicación Unir PDF.
 */
public class MainFrame extends JFrame {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainFrame.class);

    private final ConfigManager config = new ConfigManager();
    private final PdfScanner scanner = new PdfScanner();
    private final PdfMerger merger = new PdfMerger();

    // Modelos
    private final PdfTableModel tableModel = new PdfTableModel();
    private final SelectedListModel selectedModel = new SelectedListModel();
    private TableRowSorter<PdfTableModel> sorter;

    // Componentes UI
    private JTable table;
    private JList<PdfFile> selectedList;
    private PreviewPanel previewPanel;
    private JTextField filterField;
    private JLabel rootLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    public MainFrame() {
        super("Unir PDF — Combinar PDFs de múltiples carpetas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);

        buildUI();
        updateCounters();
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------------
    // Barra superior
    // ------------------------------------------------------------------
    private JComponent buildToolbar() {
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));

        // Línea 1: selección de carpeta
        JPanel folderRow = new JPanel(new BorderLayout(8, 0));
        JButton chooseBtn = new JButton("📁 Seleccionar carpeta raíz...");
        chooseBtn.addActionListener(e -> onChooseRoot());
        rootLabel = new JLabel("Carpeta: (ninguna)");
        rootLabel.setForeground(Color.DARK_GRAY);

        JButton mergeBtn = new JButton("✔ Unir PDFs seleccionados");
        mergeBtn.setFont(mergeBtn.getFont().deriveFont(Font.BOLD));
        mergeBtn.addActionListener(e -> onMerge());

        folderRow.add(chooseBtn, BorderLayout.WEST);
        folderRow.add(rootLabel, BorderLayout.CENTER);
        folderRow.add(mergeBtn, BorderLayout.EAST);

        // Línea 2: filtro + botones de selección
        JPanel filterRow = new JPanel(new BorderLayout(8, 0));
        filterField = new JTextField();
        filterField.putClientProperty("JTextField.placeholderText",
                "Filtrar por nombre, carpeta o ruta...");
        filterField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { applyFilter(); }
        });

        JPanel selectionButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton selectAll = new JButton("Seleccionar todo");
        JButton deselectAll = new JButton("Deseleccionar todo");
        JButton invert = new JButton("Invertir selección");
        selectAll.addActionListener(e -> tableModel.selectAll(true));
        deselectAll.addActionListener(e -> tableModel.selectAll(false));
        invert.addActionListener(e -> tableModel.invertSelection());
        selectionButtons.add(selectAll);
        selectionButtons.add(deselectAll);
        selectionButtons.add(invert);

        filterRow.add(new JLabel("🔍 Buscar:"), BorderLayout.WEST);
        filterRow.add(filterField, BorderLayout.CENTER);
        filterRow.add(selectionButtons, BorderLayout.EAST);

        JPanel rows = new JPanel(new GridLayout(2, 1, 0, 6));
        rows.add(folderRow);
        rows.add(filterRow);
        top.add(rows, BorderLayout.CENTER);
        return top;
    }

    // ------------------------------------------------------------------
    // Centro: tabla + vista previa + lista de orden
    // ------------------------------------------------------------------
    private JComponent buildCenter() {
        // Tabla
        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(false);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(PdfTableModel.COL_SELECTED).setMaxWidth(40);
        cm.getColumn(PdfTableModel.COL_SELECTED).setMinWidth(40);
        cm.getColumn(PdfTableModel.COL_NAME).setPreferredWidth(240);
        cm.getColumn(PdfTableModel.COL_FOLDER).setPreferredWidth(160);
        cm.getColumn(PdfTableModel.COL_PATH).setPreferredWidth(360);
        cm.getColumn(PdfTableModel.COL_SIZE).setPreferredWidth(80);
        cm.getColumn(PdfTableModel.COL_DATE).setPreferredWidth(140);

        // Click en fila => preview
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int viewRow = table.getSelectedRow();
            if (viewRow >= 0) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                previewPanel.showPreview(tableModel.getAt(modelRow));
            }
        });

        // Cambio en selección (checkbox) => sincronizar lista de orden y contadores
        tableModel.setSelectionListener(() -> {
            selectedModel.syncWith(tableModel.getSelected());
            updateCounters();
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("PDFs encontrados"));

        // Vista previa
        previewPanel = new PreviewPanel();

        // Lista de orden (seleccionados)
        selectedList = new JList<>(selectedModel);
        selectedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectedList.setDragEnabled(true);
        selectedList.setDropMode(DropMode.INSERT);
        selectedList.setTransferHandler(new ListReorderHandler());
        selectedList.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof PdfFile) {
                    PdfFile p = (PdfFile) value;
                    lbl.setText((index + 1) + ". " + p.getName()
                            + "   [" + p.getParentFolder() + "]");
                }
                return lbl;
            }
        });

        JPanel orderPanel = new JPanel(new BorderLayout(4, 4));
        orderPanel.setBorder(BorderFactory.createTitledBorder(
                "Orden de unión (arrastra o usa los botones)"));
        orderPanel.add(new JScrollPane(selectedList), BorderLayout.CENTER);

        JPanel orderButtons = new JPanel(new GridLayout(4, 1, 0, 4));
        JButton upBtn = new JButton("▲ Subir");
        JButton downBtn = new JButton("▼ Bajar");
        JButton removeBtn = new JButton("✕ Quitar");
        JButton clearBtn = new JButton("Vaciar");
        upBtn.addActionListener(e -> {
            int i = selectedList.getSelectedIndex();
            if (i > 0) { selectedModel.moveUp(i); selectedList.setSelectedIndex(i - 1); }
        });
        downBtn.addActionListener(e -> {
            int i = selectedList.getSelectedIndex();
            if (i >= 0 && i < selectedModel.size() - 1) {
                selectedModel.moveDown(i); selectedList.setSelectedIndex(i + 1);
            }
        });
        removeBtn.addActionListener(e -> {
            int i = selectedList.getSelectedIndex();
            if (i >= 0) {
                PdfFile p = selectedModel.get(i);
                p.setSelected(false);
                selectedModel.remove(i);
                tableModel.fireTableDataChanged();
                updateCounters();
            }
        });
        clearBtn.addActionListener(e -> tableModel.selectAll(false));
        orderButtons.add(upBtn);
        orderButtons.add(downBtn);
        orderButtons.add(removeBtn);
        orderButtons.add(clearBtn);

        JPanel orderRight = new JPanel(new BorderLayout(4, 4));
        orderRight.add(orderPanel, BorderLayout.CENTER);
        JPanel btnWrap = new JPanel(new BorderLayout());
        btnWrap.add(orderButtons, BorderLayout.NORTH);
        btnWrap.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        orderRight.add(btnWrap, BorderLayout.EAST);

        // Split derecho: preview + orden
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                previewPanel, orderRight);
        rightSplit.setResizeWeight(0.5);
        rightSplit.setBorder(null);

        // Split principal: tabla | (preview+orden)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                tableScroll, rightSplit);
        mainSplit.setResizeWeight(0.6);
        mainSplit.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        return mainSplit;
    }

    // ------------------------------------------------------------------
    // Barra de estado inferior
    // ------------------------------------------------------------------
    private JComponent buildStatusBar() {
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 12, 10, 12));

        statusLabel = new JLabel("Listo.");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressBar.setValue(0);

        bottom.add(statusLabel, BorderLayout.WEST);
        bottom.add(progressBar, BorderLayout.CENTER);
        return bottom;
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------
    private void onChooseRoot() {
        JFileChooser chooser = new JFileChooser(config.getLastRootFolder());
        chooser.setDialogTitle("Selecciona la carpeta raíz");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File root = chooser.getSelectedFile();
        config.setLastRootFolder(root.getAbsolutePath());
        rootLabel.setText("Carpeta: " + root.getAbsolutePath());
        scanFolder(root.toPath());
    }

    private void scanFolder(Path root) {
        statusLabel.setText("Escaneando...");
        progressBar.setIndeterminate(true);
        progressBar.setString("Buscando PDFs...");

        SwingWorker<List<PdfFile>, Void> worker = new SwingWorker<>() {
            @Override protected List<PdfFile> doInBackground() throws Exception {
                return scanner.scan(root, null);
            }
            @Override protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setString("");
                progressBar.setValue(0);
                try {
                    List<PdfFile> pdfs = get();
                    tableModel.setData(pdfs);
                    statusLabel.setText("Escaneo completado.");
                    LOGGER.info("Cargados {} PDFs en la tabla", pdfs.size());
                    updateCounters();
                } catch (Exception ex) {
                    LOGGER.error("Error escaneando carpeta", ex);
                    UiUtils.error(MainFrame.this,
                            "No se pudo escanear la carpeta:\n" + ex.getMessage());
                    statusLabel.setText("Error al escanear.");
                }
            }
        };
        worker.execute();
    }

    private void applyFilter() {
        String text = filterField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            // Filtra en nombre, carpeta y ruta (case-insensitive)
            String regex = "(?i)" + java.util.regex.Pattern.quote(text);
            sorter.setRowFilter(RowFilter.regexFilter(regex,
                    PdfTableModel.COL_NAME,
                    PdfTableModel.COL_FOLDER,
                    PdfTableModel.COL_PATH));
        }
    }

    private void updateCounters() {
        int total = tableModel.getRowCount();
        int sel = tableModel.countSelected();
        statusLabel.setText("Total: " + total + " PDFs   |   Seleccionados: " + sel);
    }

    private void onMerge() {
        List<PdfFile> ordered = selectedModel.getAll();
        if (ordered.isEmpty()) {
            UiUtils.error(this, "No has seleccionado ningún PDF.");
            return;
        }

        // Diálogo de guardado, recordando la última ubicación y nombre
        JFileChooser chooser = new JFileChooser(config.getLastOutputDir());
        chooser.setDialogTitle("Guardar PDF unido como...");
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos PDF (*.pdf)", "pdf"));
        chooser.setSelectedFile(new File(config.getLastOutputName()));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File output = chooser.getSelectedFile();
        if (!output.getName().toLowerCase().endsWith(".pdf")) {
            output = new File(output.getParentFile(), output.getName() + ".pdf");
        }
        if (output.exists() && !UiUtils.confirm(this,
                "El archivo ya existe. ¿Sobrescribir?\n" + output.getAbsolutePath())) {
            return;
        }

        // Guardar preferencias
        config.setLastOutputDir(output.getParentFile().getAbsolutePath());
        config.setLastOutputName(output.getName());

        runMergeTask(ordered, output);
    }

    private void runMergeTask(List<PdfFile> ordered, File output) {
        progressBar.setIndeterminate(false);
        progressBar.setMinimum(0);
        progressBar.setMaximum(ordered.size());
        progressBar.setValue(0);
        progressBar.setString("0 / " + ordered.size());
        statusLabel.setText("Uniendo PDFs...");

        SwingWorker<Void, int[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                merger.merge(ordered, output, (done, total) ->
                        publish(new int[]{done, total}));
                return null;
            }

            @Override
            protected void process(List<int[]> chunks) {
                int[] last = chunks.get(chunks.size() - 1);
                progressBar.setValue(last[0]);
                progressBar.setString(last[0] + " / " + last[1]);
            }

            @Override
            protected void done() {
                try {
                    get();
                    progressBar.setValue(progressBar.getMaximum());
                    progressBar.setString("Completado");
                    statusLabel.setText("PDF generado: " + output.getName());
                    LOGGER.info("PDF unido generado: {}", output.getAbsolutePath());
                    int opt = JOptionPane.showConfirmDialog(MainFrame.this,
                            "PDF generado correctamente:\n" + output.getAbsolutePath()
                                    + "\n\n¿Abrir la carpeta?",
                            "Éxito",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE);
                    if (opt == JOptionPane.YES_OPTION) {
                        openContainingFolder(output);
                    }
                } catch (Exception ex) {
                    LOGGER.error("Error uniendo PDFs", ex);
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    UiUtils.error(MainFrame.this,
                            "Error al generar el PDF:\n" + cause.getMessage());
                    statusLabel.setText("Error al generar el PDF.");
                }
                updateCounters();
            }
        };
        worker.execute();
    }

    private void openContainingFolder(File file) {
        try {
            Desktop.getDesktop().open(file.getParentFile());
        } catch (Exception ex) {
            LOGGER.warn("No se pudo abrir la carpeta: {}", ex.getMessage());
        }
    }
}
