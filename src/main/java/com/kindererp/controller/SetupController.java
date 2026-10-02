package com.kindererp.controller;

import com.kindererp.model.SchoolSettings;
import com.kindererp.service.SchoolSettingsService;
import com.kindererp.service.SchoolYearService;
import com.kindererp.service.SetupService;
import com.kindererp.service.UserService;
import com.kindererp.service.dto.SetupRequest;
import com.kindererp.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** First-launch wizard: school information, first school year and the first administrator account. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class SetupController {

    private final SetupService setupService;
    private final SchoolSettingsService settingsService;

    @FXML private ImageView productIcon;
    @FXML private ComboBox<I18n.Language> languageCombo;
    @FXML private SchoolSettingsController schoolFormController;
    @FXML private TextField yearNameField;
    @FXML private TextField adminFullNameField;
    @FXML private TextField adminUsernameField;
    @FXML private PasswordField adminPasswordField;
    @FXML private PasswordField adminPasswordConfirmField;
    @FXML private CheckBox demoDataCheck;
    @FXML private Button finishButton;

    @FXML
    private void initialize() {
        productIcon.setImage(AppState.productIcon());
        schoolFormController.setEmbedded(true);
        yearNameField.setText(SchoolYearService.suggestedName(AppState.settings().getYearStartMonth()));

        languageCombo.getItems().setAll(I18n.Language.values());
        languageCombo.setValue(I18n.language());
        languageCombo.valueProperty().addListener((obs, old, language) -> {
            if (language != null && language != I18n.language()) {
                I18n.setLanguage(language);
                Screens.showSetup();
            }
        });
    }

    @FXML
    private void handleFinish() {
        SchoolSettings settings = AppState.settings();
        if (!schoolFormController.applyTo(settings, window())) {
            return;
        }
        FormValidator validator = new FormValidator()
                .required(yearNameField, "schoolYears.name")
                .required(adminUsernameField, "login.username")
                .required(adminPasswordField, "login.password");
        validator.check(adminPasswordField, adminPasswordField.getText().length() >= UserService.MIN_PASSWORD_LENGTH,
                "validation.password.tooShortField", "login.password");
        validator.check(adminPasswordConfirmField, adminPasswordField.getText().equals(adminPasswordConfirmField.getText()),
                "validation.password.mismatch", "users.passwordConfirm");
        if (!validator.validate(window())) {
            return;
        }

        SetupRequest request = new SetupRequest(settings, adminUsernameField.getText(), adminFullNameField.getText(),
                adminPasswordField.getText(), yearNameField.getText(), demoDataCheck.isSelected());
        FxAsync.run(finishButton, () -> {
            setupService.completeSetup(request);
            return settingsService.get();
        }, saved -> {
            AppState.setSettings(saved);
            Dialogs.info(window(), I18n.get("setup.done"));
            Screens.showLogin();
        });
    }

    private javafx.stage.Window window() {
        return finishButton.getScene().getWindow();
    }
}
