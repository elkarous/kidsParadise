package com.kindererp.controller;

import com.kindererp.model.AppUser;
import com.kindererp.model.SchoolYear;
import com.kindererp.service.SchoolYearService;
import com.kindererp.service.UserService;
import com.kindererp.util.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;
    private final SchoolYearService schoolYearService;

    @FXML private ComboBox<I18n.Language> languageCombo;
    @FXML private ImageView logoView;
    @FXML private Label schoolNameLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<SchoolYear> schoolYearCombo;
    @FXML private Button loginButton;

    @FXML
    private void initialize() {
        logoView.setImage(AppState.logo());
        schoolNameLabel.setText(AppState.schoolName());

        languageCombo.getItems().setAll(I18n.Language.values());
        languageCombo.setValue(I18n.language());
        languageCombo.valueProperty().addListener((obs, old, language) -> {
            if (language != null && language != I18n.language()) {
                I18n.setLanguage(language);
                Screens.showLogin();
            }
        });

        schoolYearCombo.setConverter(ComboBoxes.converter(SchoolYear::getName));
        FxAsync.run(loginButton, () -> new YearsData(schoolYearService.findAll(), schoolYearService.findCurrent()), data -> {
            schoolYearCombo.setItems(FXCollections.observableArrayList(data.years()));
            schoolYearCombo.setValue(data.current());
        });
    }

    @FXML
    private void handleLogin() {
        FormValidator validator = new FormValidator()
                .required(usernameField, "login.username")
                .required(passwordField, "login.password")
                .required(schoolYearCombo, "login.schoolYear");
        if (!validator.validate(loginButton.getScene().getWindow())) {
            return;
        }
        String username = usernameField.getText();
        String password = passwordField.getText();
        SchoolYear year = schoolYearCombo.getValue();
        FxAsync.run(loginButton, () -> userService.authenticate(username, password), (Optional<AppUser> user) -> {
            if (user.isEmpty()) {
                passwordField.clear();
                Dialogs.warn(loginButton.getScene().getWindow(), I18n.get("login.error.invalid"));
                return;
            }
            AppState.setCurrentUser(user.get());
            AppState.setCurrentSchoolYear(year);
            Screens.showMain();
        });
    }

    private record YearsData(List<SchoolYear> years, SchoolYear current) {
    }
}
