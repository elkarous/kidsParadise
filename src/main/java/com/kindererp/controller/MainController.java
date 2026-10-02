package com.kindererp.controller;

import com.kindererp.model.SchoolSettings;
import com.kindererp.service.LicenseService;
import com.kindererp.util.FxAsync;
import com.kindererp.util.AppState;
import com.kindererp.util.I18n;
import com.kindererp.util.Screens;
import com.kindererp.util.ViewLoader;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/** Application shell: header, sidebar navigation and the content area hosting each module. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MainController {

    private final LicenseService licenseService;

    @FXML private ImageView logoView;
    @FXML private Label schoolNameLabel;
    @FXML private Label schoolYearLabel;
    @FXML private Label userLabel;
    @FXML private HBox trialBanner;
    @FXML private Label trialLabel;
    @FXML private StackPane contentArea;
    @FXML private Button navStudents;
    @FXML private Button navParents;
    @FXML private Button navTuition;
    @FXML private Button navAttendance;
    @FXML private Button navStaff;
    @FXML private Button navStaffAttendance;
    @FXML private Button navPayroll;
    @FXML private Button navSettings;

    // Held in a field: the weak listener must not be collected while this screen is shown.
    private final ChangeListener<SchoolSettings> settingsListener = (obs, old, settings) -> refreshHeader();

    @FXML
    private void initialize() {
        refreshHeader();
        AppState.settingsProperty().addListener(new WeakChangeListener<>(settingsListener));
        schoolYearLabel.setText(I18n.get("main.schoolYear", AppState.currentSchoolYear().getName()));
        var user = AppState.currentUser();
        userLabel.setText(user.getFullName() != null ? user.getFullName() : user.getUsername());
        FxAsync.run(null, licenseService::status, status -> showBanner(
                status.state() == LicenseService.State.TRIAL ? I18n.get("license.banner.trial", status.trialDaysLeft()) : null));
        showStudents();
    }

    /** Shows the trial / license warning strip under the header. */
    public void showBanner(String text) {
        trialLabel.setText(text);
        trialBanner.setVisible(text != null);
        trialBanner.setManaged(text != null);
    }

    @FXML
    private void showStudents() {
        open("students", navStudents);
    }

    @FXML
    private void showParents() {
        open("parents", navParents);
    }

    @FXML
    private void showTuition() {
        open("tuition", navTuition);
    }

    @FXML
    private void showAttendance() {
        open("attendance", navAttendance);
    }

    @FXML
    private void showStaff() {
        open("staff", navStaff);
    }

    @FXML
    private void showStaffAttendance() {
        open("staff_attendance", navStaffAttendance);
    }

    @FXML
    private void showPayroll() {
        open("payroll", navPayroll);
    }

    @FXML
    private void showSettings() {
        open("settings", navSettings);
    }

    @FXML
    private void handleLogout() {
        AppState.logout();
        Screens.showLogin();
    }

    private void refreshHeader() {
        logoView.setImage(AppState.logo());
        schoolNameLabel.setText(AppState.schoolName());
    }

    private void open(String view, Button navButton) {
        contentArea.getChildren().setAll(ViewLoader.load(view).root());
        for (Button button : List.of(navStudents, navParents, navTuition, navAttendance, navStaff,
                navStaffAttendance, navPayroll, navSettings)) {
            button.getStyleClass().remove("active");
        }
        navButton.getStyleClass().add("active");
    }
}
