package com.kindererp.controller;

import com.kindererp.config.AppPaths;
import com.kindererp.service.BackupService;
import com.kindererp.service.ExportService;
import com.kindererp.util.Dialogs;
import com.kindererp.util.FxAsync;
import com.kindererp.util.I18n;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

/** Backup / restore of the database file and export of all data to Excel. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class DataController {

    private final BackupService backupService;
    private final ExportService exportService;

    @FXML private Label locationLabel;
    @FXML private Button backupButton;
    @FXML private Button restoreButton;
    @FXML private Button exportButton;

    @FXML
    private void initialize() {
        locationLabel.setText(I18n.get("data.location", AppPaths.dbFile(), AppPaths.backupsDir()));
    }

    @FXML
    private void handleBackup() {
        FileChooser chooser = dbChooser(I18n.get("data.backup"));
        chooser.setInitialFileName(BackupService.suggestedFileName());
        File target = chooser.showSaveDialog(window());
        if (target != null) {
            FxAsync.run(backupButton, () -> backupService.backupTo(target.toPath()),
                    path -> Dialogs.info(window(), I18n.get("data.backup.done", path)));
        }
    }

    @FXML
    private void handleRestore() {
        File source = dbChooser(I18n.get("data.restore")).showOpenDialog(window());
        if (source == null || !Dialogs.confirm(window(), I18n.get("data.restore.confirm"))) {
            return;
        }
        FxAsync.runAction(restoreButton, () -> backupService.scheduleRestore(source.toPath()), () -> {
            Dialogs.info(window(), I18n.get("data.restore.restart"));
            Platform.exit();
        });
    }

    @FXML
    private void handleExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("export.button"));
        chooser.setInitialFileName(ExportService.suggestedFileName());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File target = chooser.showSaveDialog(window());
        if (target != null) {
            FxAsync.run(exportButton, () -> exportService.exportAll(target.toPath(), key -> I18n.get(key)),
                    path -> Dialogs.info(window(), I18n.get("export.done", path)));
        }
    }

    @FXML
    private void handleOpenFolder() {
        try {
            Desktop.getDesktop().open(AppPaths.backupsDir().toFile());
        } catch (IOException | UnsupportedOperationException e) {
            Dialogs.info(window(), AppPaths.backupsDir().toString());
        }
    }

    private static FileChooser dbChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialDirectory(AppPaths.backupsDir().toFile());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.get("file.database"), "*.db"));
        return chooser;
    }

    private Window window() {
        return backupButton.getScene().getWindow();
    }
}
