package com.unirpdf.model;

import java.io.File;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Representa un archivo PDF detectado por el escáner.
 * Almacena metadatos y estado de selección.
 */
public class PdfFile {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final File file;
    private final String name;
    private final String absolutePath;
    private final String parentFolder;
    private final long sizeBytes;
    private final long lastModifiedMillis;
    private boolean selected;

    /**
     * Rotaciones adicionales por página (índice 0-based -> grados {0,90,180,270}).
     * Esta rotación se suma a la del documento original al renderizar la previa
     * y al unir el PDF final.
     */
    private final Map<Integer, Integer> pageRotations = new HashMap<>();

    public PdfFile(File file) {
        this.file = Objects.requireNonNull(file, "file no puede ser null");
        this.name = file.getName();
        this.absolutePath = file.getAbsolutePath();
        File parent = file.getParentFile();
        this.parentFolder = parent != null ? parent.getName() : "";
        this.sizeBytes = file.length();
        this.lastModifiedMillis = file.lastModified();
        this.selected = false;
    }

    public File getFile() { return file; }

    public String getName() { return name; }

    public String getAbsolutePath() { return absolutePath; }

    public String getParentFolder() { return parentFolder; }

    public long getSizeBytes() { return sizeBytes; }

    public long getLastModifiedMillis() { return lastModifiedMillis; }

    public boolean isSelected() { return selected; }

    public void setSelected(boolean selected) { this.selected = selected; }

    /** Devuelve la rotación adicional aplicada por el usuario a la página (0/90/180/270). */
    public int getPageRotation(int pageIndex) {
        Integer v = pageRotations.get(pageIndex);
        return v == null ? 0 : v;
    }

    /** Reemplaza la rotación adicional de una página. */
    public void setPageRotation(int pageIndex, int degrees) {
        int norm = ((degrees % 360) + 360) % 360;
        if (norm == 0) {
            pageRotations.remove(pageIndex);
        } else {
            pageRotations.put(pageIndex, norm);
        }
    }

    /** Rota una página 90 grados en sentido horario y devuelve la nueva rotación. */
    public int rotatePageClockwise(int pageIndex) {
        int next = (getPageRotation(pageIndex) + 90) % 360;
        setPageRotation(pageIndex, next);
        return next;
    }

    /** Rota una página 90 grados en sentido antihorario y devuelve la nueva rotación. */
    public int rotatePageCounterClockwise(int pageIndex) {
        int next = (getPageRotation(pageIndex) + 270) % 360;
        setPageRotation(pageIndex, next);
        return next;
    }

    public Map<Integer, Integer> getPageRotations() {
        return pageRotations;
    }

    /** Tamaño formateado en KB / MB. */
    public String getSizeFormatted() {
        if (sizeBytes < 1024) return sizeBytes + " B";
        double kb = sizeBytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        return String.format("%.2f MB", mb);
    }

    /** Fecha de modificación formateada. */
    public String getLastModifiedFormatted() {
        LocalDateTime dt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(lastModifiedMillis),
                ZoneId.systemDefault());
        return dt.format(DATE_FORMAT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PdfFile)) return false;
        PdfFile other = (PdfFile) o;
        return absolutePath.equals(other.absolutePath);
    }

    @Override
    public int hashCode() {
        return absolutePath.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
