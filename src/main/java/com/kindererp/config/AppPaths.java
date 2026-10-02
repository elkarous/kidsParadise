package com.kindererp.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where the application keeps its data. Never inside the install folder (read-only once installed).
 *
 * <p>Resolution order for the data folder:
 * <ol>
 *   <li>system property {@code kindererp.home} (e.g. {@code -Dkindererp.home=D:\KinderERP})</li>
 *   <li>environment variable {@code KINDERERP_HOME}</li>
 *   <li>{@code %APPDATA%\KinderERP} on Windows, {@code ~/.kindererp} elsewhere</li>
 * </ol>
 * The database file itself can be overridden with {@code -Dkindererp.db=path\to\file.db}.
 */
public final class AppPaths {

    public static final String APP_NAME = "KinderERP";
    public static final String DB_FILE_NAME = "kindererp.db";

    private static Path home;

    private AppPaths() {
    }

    /** Resolves the folders and exposes the log file location to the logging configuration. */
    public static void init() {
        System.setProperty("kindererp.log.file", logsDir().resolve("kindererp.log").toString());
    }

    public static synchronized Path home() {
        if (home == null) {
            home = createDirectories(resolveHome());
        }
        return home;
    }

    public static Path dbFile() {
        String override = System.getProperty("kindererp.db");
        return override != null && !override.isBlank() ? Path.of(override).toAbsolutePath() : home().resolve(DB_FILE_NAME);
    }

    public static Path backupsDir() {
        return createDirectories(home().resolve("backups"));
    }

    public static Path logsDir() {
        return createDirectories(home().resolve("logs"));
    }

    private static Path resolveHome() {
        String property = System.getProperty("kindererp.home");
        if (property != null && !property.isBlank()) {
            return Path.of(property);
        }
        String env = System.getenv("KINDERERP_HOME");
        if (env != null && !env.isBlank()) {
            return Path.of(env);
        }
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, APP_NAME);
        }
        return Path.of(System.getProperty("user.home"), ".kindererp");
    }

    private static Path createDirectories(Path dir) {
        try {
            return Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create folder " + dir, e);
        }
    }
}
