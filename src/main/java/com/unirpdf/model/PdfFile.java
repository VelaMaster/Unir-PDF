package com.unirpdf.model;

import java.io.File;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
