package com.kindererp;

import com.kindererp.config.AppPaths;
import com.kindererp.service.BackupService;
import com.kindererp.service.LicenseService;
import com.kindererp.service.SchoolSettingsService;
import com.kindererp.util.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.InputStream;

/** JavaFX application: starts Spring (database, services), then shows setup, login or the main window. */
@Slf4j
public class FxApplication extends Application {

    private ConfigurableApplicationContext context;
    private Throwable startupError;

    @Override
    public void init() {
        try {
            context = new SpringApplicationBuilder(KinderErpApplication.class).headless(false).run();
            ViewLoader.setContext(context);
        } catch (Throwable e) {
            startupError = e;
        }
    }

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            log.error("Uncaught error on {}", thread.getName(), error);
            if (Platform.isFxApplicationThread()) {
                Dialogs.showError(null, error);
            }
        });
        loadFonts();

        if (startupError != null) {
            log.error("Startup failed", startupError);
            Alert alert = new Alert(Alert.AlertType.ERROR, I18n.get("error.startup", logsFolder()));
            alert.setHeaderText(null);
            alert.showAndWait();
            Platform.exit();
            return;
        }

        SchoolSettingsService settingsService = context.getBean(SchoolSettingsService.class);
        AppState.setSettings(settingsService.get());
        Screens.init(stage);

        if (!settingsService.isSetupCompleted()) {
            Screens.showSetup();
        } else {
            LicenseService.Status license = context.getBean(LicenseService.class).status();
            if (license.allowsUse()) {
                Screens.showLogin();
            } else {
                Screens.showActivation();
            }
            FxAsync.runAction(null, () -> context.getBean(BackupService.class).dailyAutoBackup(), null);
        }
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }

    private static String logsFolder() {
        return AppPaths.logsDir().toString();
    }

    private static void loadFonts() {
        for (String font : new String[]{"/fonts/Tajawal-Regular.ttf", "/fonts/Tajawal-Bold.ttf"}) {
            try (InputStream in = FxApplication.class.getResourceAsStream(font)) {
                if (in == null || Font.loadFont(in, 13) == null) {
                    log.warn("Could not load font {}", font);
                }
            } catch (Exception e) {
                log.warn("Could not load font {}", font, e);
            }
        }
    }
}
