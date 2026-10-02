package com.kindererp.controller;

import com.kindererp.model.AppUser;
import com.kindererp.service.UserService;
import com.kindererp.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Application accounts: add, reset password, activate / deactivate, delete. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class UsersController {

    private final UserService userService;

    @FXML private TableView<AppUser> table;
    @FXML private TableColumn<AppUser, String> usernameColumn;
    @FXML private TableColumn<AppUser, String> fullNameColumn;
    @FXML private TableColumn<AppUser, AppUser> activeColumn;
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField passwordConfirmField;
    @FXML private Button addButton;

    @FXML
    private void initialize() {
        Tables.text(usernameColumn, AppUser::getUsername);
        Tables.text(fullNameColumn, AppUser::getFullName);
        Tables.badge(activeColumn, u -> I18n.get(u.isActive() ? "users.active" : "users.inactive"),
                u -> u.isActive() ? "badge-success" : "badge-neutral");
        load();
    }

    private void load() {
        FxAsync.run(table, userService::findAll, users -> table.getItems().setAll(users));
    }

    @FXML
    private void handleAdd() {
        FormValidator validator = new FormValidator()
                .required(usernameField, "login.username")
                .required(passwordField, "login.password");
        validator.check(passwordField, passwordField.getText().length() >= UserService.MIN_PASSWORD_LENGTH,
                "validation.password.tooShortField", "login.password");
        validator.check(passwordConfirmField, passwordField.getText().equals(passwordConfirmField.getText()),
                "validation.password.mismatch", "users.passwordConfirm");
        if (!validator.validate(window())) {
            return;
        }
        String username = usernameField.getText();
        String fullName = fullNameField.getText();
        String password = passwordField.getText();
        FxAsync.run(addButton, () -> userService.createUser(username, fullName, password), user -> {
            usernameField.clear();
            fullNameField.clear();
            passwordField.clear();
            passwordConfirmField.clear();
            load();
        });
    }

    @FXML
    private void handleResetPassword() {
        AppUser user = table.getSelectionModel().getSelectedItem();
        if (user == null) {
            return;
        }
        askNewPassword(user).ifPresent(password ->
                FxAsync.runAction(table, () -> userService.changePassword(user.getId(), password),
                        () -> Dialogs.info(window(), I18n.get("users.passwordChanged"))));
    }

    @FXML
    private void handleToggleActive() {
        AppUser user = table.getSelectionModel().getSelectedItem();
        if (user != null) {
            FxAsync.runAction(table, () -> userService.setActive(user.getId(), !user.isActive()), this::load);
        }
    }

    @FXML
    private void handleDelete() {
        AppUser user = table.getSelectionModel().getSelectedItem();
        if (user == null) {
            return;
        }
        if (user.getId().equals(AppState.currentUser().getId())) {
            Dialogs.warn(window(), I18n.get("users.error.self"));
            return;
        }
        if (Dialogs.confirm(window(), I18n.get("users.delete.confirm", user.getUsername()))) {
            FxAsync.runAction(table, () -> userService.delete(user.getId()), this::load);
        }
    }

    private Optional<String> askNewPassword(AppUser user) {
        PasswordField first = new PasswordField();
        PasswordField second = new PasswordField();
        Dialog<String> dialog = new Dialog<>();
        dialog.initOwner(window());
        dialog.setTitle(I18n.get("users.resetPassword"));
        dialog.setHeaderText(user.getUsername());
        dialog.getDialogPane().setNodeOrientation(I18n.orientation());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource(ViewLoader.STYLESHEET).toExternalForm());
        dialog.getDialogPane().setContent(new VBox(8, new Label(I18n.get("users.newPassword")), first,
                new Label(I18n.get("users.passwordConfirm")), second));
        ButtonType ok = new ButtonType(I18n.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, new ButtonType(I18n.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            FormValidator validator = new FormValidator();
            validator.check(first, first.getText().length() >= UserService.MIN_PASSWORD_LENGTH,
                    "validation.password.tooShortField", "users.newPassword");
            validator.check(second, first.getText().equals(second.getText()), "validation.password.mismatch", "users.passwordConfirm");
            if (!validator.validate(dialog.getDialogPane().getScene().getWindow())) {
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == ok ? first.getText() : null);
        return dialog.showAndWait();
    }

    private Window window() {
        return table.getScene().getWindow();
    }
}
