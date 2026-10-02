package com.kindererp.util;

import com.kindererp.service.BusinessException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/** Translated, right-to-left aware message boxes. Technical details go to the log file, never to the user. */
@Slf4j
public final class Dialogs {

    private Dialogs() {
    }

    public static void info(Window owner, String text) {
        show(Alert.AlertType.INFORMATION, owner, I18n.get("dialog.info.title"), text);
    }

    public static void warn(Window owner, String text) {
        show(Alert.AlertType.WARNING, owner, I18n.get("dialog.warning.title"), text);
    }

    public static void error(Window owner, String text) {
        show(Alert.AlertType.ERROR, owner, I18n.get("dialog.error.title"), text);
    }

    public static void validation(Window owner, List<String> problems) {
        show(Alert.AlertType.WARNING, owner, I18n.get("dialog.validation.title"),
                I18n.get("dialog.validation.header") + "\n\n• " + String.join("\n• ", problems));
    }

    public static boolean confirm(Window owner, String text) {
        ButtonType yes = new ButtonType(I18n.get("btn.yes"), ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType(I18n.get("btn.no"), ButtonBar.ButtonData.NO);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, yes, no);
        prepare(alert, owner, I18n.get("dialog.confirm.title"));
        return alert.showAndWait().filter(yes::equals).isPresent();
    }

    /** Shows a business error in the user's language, or a generic message (and logs) for anything else. */
    public static void showError(Window owner, Throwable error) {
        Throwable cause = unwrap(error);
        if (cause instanceof BusinessException business) {
            warn(owner, I18n.get(business.getMessageKey(), business.getArgs()));
        } else {
            log.error("Unexpected error", cause);
            error(owner, I18n.get("error.unexpected"));
        }
    }

    private static void show(Alert.AlertType type, Window owner, String title, String text) {
        Alert alert = new Alert(type, text, new ButtonType(I18n.get("btn.ok"), ButtonBar.ButtonData.OK_DONE));
        prepare(alert, owner, title);
        alert.showAndWait();
    }

    private static void prepare(Alert alert, Window owner, String title) {
        Window resolvedOwner = owner != null ? owner : focusedWindow();
        if (resolvedOwner != null) {
            alert.initOwner(resolvedOwner);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        DialogPane pane = alert.getDialogPane();
        pane.setNodeOrientation(I18n.orientation());
        pane.getStylesheets().add(Objects.requireNonNull(Dialogs.class.getResource(ViewLoader.STYLESHEET)).toExternalForm());
        pane.setMinWidth(380);
        if (pane.getScene().getWindow() instanceof Stage stage) {
            ViewLoader.decorate(stage);
        }
    }

    private static Window focusedWindow() {
        return Window.getWindows().stream().filter(Window::isFocused).findFirst().orElse(null);
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && !(current instanceof BusinessException)) {
            current = current.getCause();
        }
        return current instanceof BusinessException ? current : error;
    }
}
