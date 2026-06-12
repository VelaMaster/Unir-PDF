package com.unirpdf.ui;

import com.unirpdf.model.PdfFile;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo de árbol que representa las ubicaciones (carpetas raíz) añadidas y
 * sus subcarpetas, construido a partir de la lista de PdfFiles encontrados.
 *
 * <p>Cada nodo almacena un {@link FolderNode}. El usuario puede:
 *  - seleccionar la raíz "Todas las ubicaciones" para ver todos los PDFs;
 *  - seleccionar una carpeta raíz o subcarpeta para filtrar la tabla a esa
 *    ubicación (incluyendo subcarpetas).
 */
public class FolderTreeModel extends DefaultTreeModel {

    public static final String ALL_LABEL = "Todas las ubicaciones";
    public static final String LOOSE_FILES_LABEL = "Archivos sueltos";

    public FolderTreeModel() {
        super(new DefaultMutableTreeNode(new FolderNode(ALL_LABEL, null, FolderNode.Kind.ALL)));
    }

    /**
     * Reconstruye el árbol a partir de las raíces añadidas y todos los PDFs detectados.
     *
     * @param roots    rutas absolutas que el usuario añadió como carpeta raíz
     * @param allPdfs  todos los PDFs actualmente en la tabla
     */
    public void rebuild(List<File> roots, List<PdfFile> allPdfs) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(
                new FolderNode(ALL_LABEL + " (" + allPdfs.size() + ")", null, FolderNode.Kind.ALL));

        // Para cada raíz, agrupar los PDFs cuya ruta empieza con esa raíz.
        List<File> sortedRoots = new ArrayList<>(roots);
        sortedRoots.sort(Comparator.comparing(File::getAbsolutePath));

        List<PdfFile> looseFiles = new ArrayList<>(allPdfs);

        for (File root : sortedRoots) {
            String rootPath = root.getAbsolutePath();
            List<PdfFile> inRoot = new ArrayList<>();
            for (PdfFile p : allPdfs) {
                if (p.getAbsolutePath().startsWith(rootPath + File.separator)
                        || p.getAbsolutePath().equals(rootPath)) {
                    inRoot.add(p);
                }
            }
            looseFiles.removeAll(inRoot);

            DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode(
                    new FolderNode(root.getName().isEmpty() ? rootPath : root.getName()
                            + " (" + inRoot.size() + ")",
                            root, FolderNode.Kind.ROOT));
            addSubFolders(rootTreeNode, root, inRoot);
            rootNode.add(rootTreeNode);
        }

        if (!looseFiles.isEmpty()) {
            DefaultMutableTreeNode loose = new DefaultMutableTreeNode(
                    new FolderNode(LOOSE_FILES_LABEL + " (" + looseFiles.size() + ")",
                            null, FolderNode.Kind.LOOSE));
            rootNode.add(loose);
        }

        setRoot(rootNode);
    }

    private void addSubFolders(DefaultMutableTreeNode parentNode, File rootDir,
                               List<PdfFile> pdfs) {
        // Construir un mapa: ruta de carpeta -> lista de PDFs directamente contenidos.
        Map<String, List<PdfFile>> byFolder = new HashMap<>();
        for (PdfFile p : pdfs) {
            File parent = p.getFile().getParentFile();
            if (parent == null) continue;
            String key = parent.getAbsolutePath();
            byFolder.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }
        // Recoger todas las carpetas únicas y sus ancestros hasta el rootDir.
        Map<String, DefaultMutableTreeNode> nodes = new HashMap<>();
        nodes.put(rootDir.getAbsolutePath(), parentNode);

        List<String> sortedFolders = new ArrayList<>(byFolder.keySet());
        sortedFolders.sort(Comparator.naturalOrder());

        for (String folderPath : sortedFolders) {
            ensureFolderNode(folderPath, rootDir, nodes);
        }
    }

    private DefaultMutableTreeNode ensureFolderNode(String folderPath, File rootDir,
                                                    Map<String, DefaultMutableTreeNode> nodes) {
        DefaultMutableTreeNode existing = nodes.get(folderPath);
        if (existing != null) return existing;
        File folder = new File(folderPath);
        File parentFolder = folder.getParentFile();
        DefaultMutableTreeNode parentNode;
        if (parentFolder == null
                || !folderPath.startsWith(rootDir.getAbsolutePath())
                || folderPath.equals(rootDir.getAbsolutePath())) {
            parentNode = nodes.get(rootDir.getAbsolutePath());
        } else {
            parentNode = ensureFolderNode(parentFolder.getAbsolutePath(), rootDir, nodes);
        }
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                new FolderNode(folder.getName(), folder, FolderNode.Kind.SUBFOLDER));
        parentNode.add(node);
        nodes.put(folderPath, node);
        return node;
    }

    /** Camino al primer nodo (raíz general "Todas las ubicaciones"). */
    public TreePath rootPath() {
        return new TreePath(getRoot());
    }

    /** Datos asociados a cada nodo del árbol. */
    public static class FolderNode {
        public enum Kind { ALL, ROOT, SUBFOLDER, LOOSE }

        private final String label;
        private final File folder;
        private final Kind kind;

        public FolderNode(String label, File folder, Kind kind) {
            this.label = label;
            this.folder = folder;
            this.kind = kind;
        }

        public File getFolder() { return folder; }
        public Kind getKind() { return kind; }

        @Override
        public String toString() { return label; }
    }
}
