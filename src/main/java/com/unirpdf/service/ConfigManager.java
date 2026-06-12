package com.unirpdf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * Persiste preferencias de usuario en %USERPROFILE%/.unirpdf/config.properties.
 * Recuerda última carpeta de escaneo y última ubicación de salida.
 */
public class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);

    private static final String KEY_LAST_ROOT = "last.root.folder";
    private static final String KEY_LAST_OUTPUT_DIR = "last.output.dir";
    private static final String KEY_LAST_OUTPUT_NAME = "last.output.name";

    private final File configFile;
    private final Properties props = new Properties();

    public ConfigManager() {
        File dir = new File(System.getProperty("user.home"), ".unirpdf");
        if (!dir.exists() && !dir.mkdirs()) {
            LOGGER.warn("No se pudo crear el directorio de configuración: {}", dir);
        }
        this.configFile = new File(dir, "config.properties");
        load();
    }

    private void load() {
        if (!configFile.exists()) return;
        try (InputStream in = new FileInputStream(configFile)) {
            props.load(in);
        } catch (IOException ex) {
            LOGGER.warn("No se pudo leer la configuración: {}", ex.getMessage());
        }
    }

    private void save() {
        try (OutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Unir PDF — preferencias de usuario");
        } catch (IOException ex) {
            LOGGER.warn("No se pudo guardar la configuración: {}", ex.getMessage());
        }
    }

    public String getLastRootFolder() {
        return props.getProperty(KEY_LAST_ROOT, System.getProperty("user.home"));
    }

    public void setLastRootFolder(String path) {
        props.setProperty(KEY_LAST_ROOT, path);
        save();
    }

    public String getLastOutputDir() {
        return props.getProperty(KEY_LAST_OUTPUT_DIR, System.getProperty("user.home"));
    }

    public void setLastOutputDir(String path) {
        props.setProperty(KEY_LAST_OUTPUT_DIR, path);
        save();
    }

    public String getLastOutputName() {
        return props.getProperty(KEY_LAST_OUTPUT_NAME, "PDF_Unido.pdf");
    }

    public void setLastOutputName(String name) {
        props.setProperty(KEY_LAST_OUTPUT_NAME, name);
        save();
    }
}
