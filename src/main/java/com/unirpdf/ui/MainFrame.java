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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ventana principal de la aplicación Unir PDF.
 *
 * <p>Soporta múltiples ubicaciones: el usuario puede añadir varias carpetas
 * raíz y archivos PDF sueltos desde cualquier parte del sistema. Un árbol a
 * la izquierda permite navegar las carpetas y filtrar la tabla por
 * ubicación. Las selecciones (checkbox) se mantienen al cambiar de carpeta.
 */
public class MainFrame extends JFrame {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainFrame.class);

    private final ConfigManager config = new ConfigManager();
    private final PdfScanner scanner = new PdfScanner();
    private final PdfMerger merger = new PdfMerger();

    // Modelos
    private final PdfTableModel tableModel = new PdfTableModel();
    private final SelectedListModel selectedModel = new SelectedListModel();
    private final FolderTreeModel folderTreeModel = new FolderTreeModel();
    private final Set<File> roots = new LinkedHashSet<>();
    private TableRowSorter<PdfTableModel> sorter;

    // Componentes UI
    private JTable table;
    private JTree folderTree;
    private JList<PdfFile> selectedList;
    private PreviewPanel previewPanel;
    private JTextField filterField;
    private JLabel rootLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    // Filtros activos (texto + carpeta seleccionada en el árbol)
    private String folderFilterPrefix; // null = sin filtro de carpeta

    public MainFrame() {
        super("Unir PDF — Combinar PDFs de múltiples carpetas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 820);
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

        // Línea 1: ubicaciones
        JPanel folderRow = new JPanel(new BorderLayout(8, 0));

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton addFolderBtn = new JButton("📁+ Agregar carpeta...");
        JButton addFilesBtn = new JButton("📄+ Agregar PDFs...");
        JButton clearBtn = new JButton("🗑 Limpiar todo");
        addFolderBtn.addActionListener(e -> onAddFolder());
        addFilesBtn.addActionListener(e -> onAddFiles());
        clearBtn.addActionListener(e -> onClearAll());
        leftBtns.add(addFolderBtn);
        leftBtns.add(addFilesBtn);
        leftBtns.add(clearBtn);

        rootLabel = new JLabel("Sin ubicaciones añadidas.");
        rootLabel.setForeground(Color.DARK_GRAY);
        rootLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        JButton mergeBtn = new JButton("✔ Unir PDFs seleccionados");
        mergeBtn.setFont(mergeBtn.getFont().deriveFont(Font.BOLD));
        mergeBtn.addActionListener(e -> onMerge());

        folderRow.add(leftBtns, BorderLayout.WEST);
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
    // Centro: árbol | tabla | (vista previa + orden)
    // ------------------------------------------------------------------
    private JComponent buildCenter() {
        // ----- Árbol de carpetas -----
        folderTree = new JTree(folderTreeModel);
        folderTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        folderTree.setRootVisible(true);
        folderTree.setShowsRootHandles(true);
        folderTree.addTreeSelectionListener(e -> onTreeSelection());

        JScrollPane treeScroll = new JScrollPane(folderTree);
        treeScroll.setBorder(BorderFactory.createTitledBorder("Ubicaciones"));
        treeScroll.setPreferredSize(new Dimension(240, 100));

        // ----- Tabla -----
        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(false);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(PdfTableModel.COL_SELECTED).setMaxWidth(40);
        cm.getColumn(PdfTableModel.COL_SELECTED).setMinWidth(40);
        cm.getColumn(PdfTableModel.COL_NAME).setPreferredWidth(220);
        cm.getColumn(PdfTableModel.COL_FOLDER).setPreferredWidth(140);
        cm.getColumn(PdfTableModel.COL_PATH).setPreferredWidth(320);
        cm.getColumn(PdfTableModel.COL_SIZE).setPreferredWidth(70);
        cm.getColumn(PdfTableModel.COL_DATE).setPreferredWidth(130);

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

        // Split izquierdo: árbol | tabla
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                treeScroll, tableScroll);
        leftSplit.setResizeWeight(0.22);
        leftSplit.setBorder(null);

        // ----- Vista previa -----
        previewPanel = new PreviewPanel();

        // ----- Lista de orden -----
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
        JButton clearListBtn = new JButton("Vaciar");
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
        clearListBtn.addActionListener(e -> tableModel.selectAll(false));
        orderButtons.add(upBtn);
        orderButtons.add(downBtn);
        orderButtons.add(removeBtn);
        orderButtons.add(clearListBtn);

        JPanel orderRight = new JPanel(new BorderLayout(4, 4));
        orderRight.add(orderPanel, BorderLayout.CENTER);
        JPanel btnWrap = new JPanel(new BorderLayout());
        btnWrap.add(orderButtons, BorderLayout.NORTH);
        btnWrap.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        orderRight.add(btnWrap, BorderLayout.EAST);

        // Split derecho: preview + orden
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                previewPanel, orderRight);
        rightSplit.setResizeWeight(0.55);
        rightSplit.setBorder(null);

        // Split principal: (árbol|tabla) | (preview+orden)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftSplit, rightSplit);
        mainSplit.setResizeWeight(0.65);
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
    // Acciones de ubicaciones
    // ------------------------------------------------------------------
    private void onAddFolder() {
        JFileChooser chooser = new JFileChooser(config.getLastRootFolder());
        chooser.setDialogTitle("Selecciona una carpeta para añadir");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File[] selected = chooser.getSelectedFiles();
        if (selected == null || selected.length == 0) {
            File single = chooser.getSelectedFile();
            if (single == null) return;
            selected = new File[]{single};
        }

        config.setLastRootFolder(selected[0].getAbsolutePath());
        for (File f : selected) addRoot(f.toPath());
    }

    private void addRoot(Path root) {
        File rootFile = root.toFile();
        if (!roots.add(rootFile)) {
            UiUtils.error(this, "Esa carpeta ya está añadida:\n" + rootFile.getAbsolutePath());
            return;
        }
        statusLabel.setText("Escaneando: " + rootFile.getAbsolutePath());
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
                    int added = tableModel.addAll(pdfs);
                    LOGGER.info("Añadidos {} PDFs desde {}", added, rootFile);
                    rebuildFolderTree();
                    refreshRootLabel();
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

    private void onAddFiles() {
        JFileChooser chooser = new JFileChooser(config.getLastRootFolder());
        chooser.setDialogTitle("Selecciona uno o más PDFs para añadir");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos PDF (*.pdf)", "pdf"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File[] files = chooser.getSelectedFiles();
        if (files == null || files.length == 0) return;

        List<PdfFile> toAdd = new ArrayList<>();
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".pdf")) {
                toAdd.add(new PdfFile(f));
            }
        }
        int added = tableModel.addAll(toAdd);
        if (added > 0) {
            config.setLastRootFolder(files[0].getParentFile().getAbsolutePath());
            rebuildFolderTree();
            refreshRootLabel();
            updateCounters();
            statusLabel.setText("Añadidos " + added + " archivo(s).");
        } else {
            statusLabel.setText("No se añadió ningún archivo (¿ya estaban?).");
        }
    }

    private void onClearAll() {
        if (!UiUtils.confirm(this,
                "¿Quitar todas las ubicaciones y la lista de PDFs?")) return;
        roots.clear();
        tableModel.clear();
        selectedModel.clear();
        folderFilterPrefix = null;
        rebuildFolderTree();
        refreshRootLabel();
        previewPanel.showPreview(null);
        updateCounters();
    }

    private void rebuildFolderTree() {
        folderTreeModel.rebuild(new ArrayList<>(roots), tableModel.getAll());
        // Expandir la raíz por defecto
        folderTree.expandPath(folderTreeModel.rootPath());
        applyFilter();
    }

    private void refreshRootLabel() {
        int n = roots.size();
        if (n == 0) {
            rootLabel.setText("Sin ubicaciones añadidas.");
        } else if (n == 1) {
            rootLabel.setText("1 ubicación: " + roots.iterator().next().getAbsolutePath());
        } else {
            rootLabel.setText(n + " ubicaciones añadidas. Usa el árbol para navegar.");
        }
    }

    private void onTreeSelection() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
        if (node == null) {
            folderFilterPrefix = null;
        } else {
            Object userObj = node.getUserObject();
            if (userObj instanceof FolderTreeModel.FolderNode) {
                FolderTreeModel.FolderNode fn = (FolderTreeModel.FolderNode) userObj;
                if (fn.getKind() == FolderTreeModel.FolderNode.Kind.ALL
                        || fn.getKind() == FolderTreeModel.FolderNode.Kind.LOOSE
                        || fn.getFolder() == null) {
                    folderFilterPrefix = null;
                } else {
                    folderFilterPrefix = fn.getFolder().getAbsolutePath();
                }
            } else {
                folderFilterPrefix = null;
            }
        }
        applyFilter();
    }

    // ------------------------------------------------------------------
    // Filtro combinado: texto + carpeta del árbol
    // ------------------------------------------------------------------
    private void applyFilter() {
        final String text = filterField.getText().trim();
        final String prefix = folderFilterPrefix;
        if (text.isEmpty() && prefix == null) {
            sorter.setRowFilter(null);
            return;
        }
        sorter.setRowFilter(new RowFilter<PdfTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends PdfTableModel, ? extends Integer> entry) {
                PdfFile p = entry.getModel().getAt(entry.getIdentifier());
                if (prefix != null) {
                    String abs = p.getAbsolutePath();
                    if (!(abs.equals(prefix) || abs.startsWith(prefix + File.separator))) {
                        return false;
                    }
                }
                if (!text.isEmpty()) {
                    String low = text.toLowerCase();
                    if (!p.getName().toLowerCase().contains(low)
                            && !p.getParentFolder().toLowerCase().contains(low)
                            && !p.getAbsolutePath().toLowerCase().contains(low)) {
                        return false;
                    }
                }
                return true;
            }
        });
    }

    private void updateCounters() {
        int total = tableModel.getRowCount();
        int sel = tableModel.countSelected();
        statusLabel.setText("Total: " + total + " PDFs   |   Seleccionados: " + sel);
    }

    // ------------------------------------------------------------------
    // Acción: Unir
    // ------------------------------------------------------------------
    private void onMerge() {
        List<PdfFile> ordered = selectedModel.getAll();
        if (ordered.isEmpty()) {
            UiUtils.error(this, "No has seleccionado ningún PDF.");
            return;
        }

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
