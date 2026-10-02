package com.kindererp.service;

import com.kindererp.config.AppPaths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

/**
 * Copies of the database file: manual backups, a daily automatic backup kept for 7 days, and restore.
 *
 * <p>A restore cannot replace the file while the application has it open, so the chosen backup is
 * staged next to the database and swapped in at the next start ({@link #applyPendingRestore}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    public static final int AUTO_BACKUP_DAYS_KEPT = 7;
    private static final String AUTO_PREFIX = "auto-";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final JdbcTemplate jdbc;

    public static String suggestedFileName() {
        return "kindererp-backup-" + LocalDateTime.now().format(STAMP) + ".db";
    }

    /** Writes a consistent copy of the live database (safe while the app is running). */
    public Path backupTo(Path target) {
        try {
            Files.createDirectories(target.toAbsolutePath().getParent());
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new BusinessException("backup.error.write", target);
        }
        jdbc.execute("VACUUM INTO '" + target.toAbsolutePath().toString().replace("'", "''") + "'");
        log.info("Database backed up to {}", target);
        return target;
    }

    /** Once a day at startup: backups/auto-yyyy-MM-dd.db, keeping the last 7 days. */
    public void dailyAutoBackup() {
        Path dir = AppPaths.backupsDir();
        Path today = dir.resolve(AUTO_PREFIX + LocalDate.now() + ".db");
        if (!Files.exists(today)) {
            backupTo(today);
        }
        LocalDate oldestKept = LocalDate.now().minusDays(AUTO_BACKUP_DAYS_KEPT - 1L);
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(f -> f.getFileName().toString().startsWith(AUTO_PREFIX))
                    .filter(f -> autoBackupDate(f) != null && autoBackupDate(f).isBefore(oldestKept))
                    .forEach(BackupService::deleteQuietly);
        } catch (IOException e) {
            log.warn("Could not clean old automatic backups", e);
        }
    }

    /** Checks the file is a KinderERP database, then stages it to replace the current one at next start. */
    public void scheduleRestore(Path backupFile) {
        if (!isKinderErpDatabase(backupFile)) {
            throw new BusinessException("backup.error.invalidFile");
        }
        try {
            Files.copy(backupFile, pendingRestoreFile(AppPaths.dbFile()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("backup.error.write", backupFile);
        }
        log.info("Restore of {} scheduled for next start", backupFile);
    }

    /** Called before anything opens the database. Keeps the replaced database in the backups folder. */
    public static void applyPendingRestore(Path dbFile) {
        Path pending = pendingRestoreFile(dbFile);
        if (!Files.exists(pending)) {
            return;
        }
        try {
            if (Files.exists(dbFile)) {
                Path saved = AppPaths.backupsDir().resolve("before-restore-" + LocalDateTime.now().format(STAMP) + ".db");
                Files.move(dbFile, saved);
            }
            deleteQuietly(dbFile.resolveSibling(dbFile.getFileName() + "-wal"));
            deleteQuietly(dbFile.resolveSibling(dbFile.getFileName() + "-shm"));
            Files.move(pending, dbFile);
        } catch (IOException e) {
            throw new IllegalStateException("Could not restore the database from " + pending, e);
        }
    }

    static boolean isKinderErpDatabase(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return false;
        }
        String url = "jdbc:sqlite:file:" + file.toAbsolutePath().toString().replace('\\', '/') + "?mode=ro";
        try (Connection connection = DriverManager.getConnection(url);
             ResultSet rs = connection.createStatement().executeQuery(
                     "select count(*) from sqlite_master where type = 'table' and name in ('school_settings', 'flyway_schema_history')")) {
            return rs.next() && rs.getInt(1) == 2;
        } catch (SQLException e) {
            return false;
        }
    }

    private static Path pendingRestoreFile(Path dbFile) {
        return dbFile.resolveSibling(dbFile.getFileName() + ".restore");
    }

    private static LocalDate autoBackupDate(Path file) {
        String name = file.getFileName().toString();
        try {
            return LocalDate.parse(name.substring(AUTO_PREFIX.length(), name.length() - ".db".length()));
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete {}", file, e);
        }
    }
}
