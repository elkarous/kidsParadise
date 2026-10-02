package com.kindererp;

import com.kindererp.config.AppPaths;
import com.kindererp.service.BackupService;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Kept separate from the JavaFX {@link Application} class so the app also starts from a
 * plain classpath (installer, IDE) without the JavaFX launcher.
 */
@SpringBootApplication
public class KinderErpApplication {

    public static void main(String[] args) {
        AppPaths.init();
        BackupService.applyPendingRestore(AppPaths.dbFile());
        Application.launch(FxApplication.class, args);
    }
}
