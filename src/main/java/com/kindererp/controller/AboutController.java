package com.kindererp.controller;

import com.kindererp.service.LicenseService;
import com.kindererp.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Clipboard;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Version and license: shows the machine ID and activates a license key. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class AboutController {

    private final LicenseService licenseService;

    @FXML private ImageView productIcon;
    @FXML private Label productLabel;
    @FXML private Label versionLabel;
    @FXML private Label statusLabel;
    @FXML private TextField machineIdField;
    @FXML private TextArea keyField;
    @FXML private Button activateButton;

    /** Called after a successful activation (the activation screen moves on to the login). */
    @Setter
    private Runnable onActivated;

    @FXML
    private void initialize() {
        productIcon.setImage(AppState.productIcon());
        productLabel.setText(AppInfo.name());
        versionLabel.setText(I18n.get("about.version", AppInfo.version()));
        refresh();
    }

    private void refresh() {
        FxAsync.run(activateButton, licenseService::status, status -> {
            machineIdField.setText(status.machineId());
            statusLabel.getStyleClass().removeAll("badge-success", "badge-warning", "badge-danger");
            switch (status.state()) {
                case LICENSED -> {
                    statusLabel.setText(I18n.get("license.state.licensed"));
                    statusLabel.getStyleClass().add("badge-success");
                }
                case TRIAL -> {
                    statusLabel.setText(I18n.get("license.state.trial", status.trialDaysLeft()));
                    statusLabel.getStyleClass().add("badge-warning");
                }
                case EXPIRED -> {
                    statusLabel.setText(I18n.get("license.state.expired"));
                    statusLabel.getStyleClass().add("badge-danger");
                }
            }
        });
    }

    @FXML
    private void handleCopy() {
        ClipboardContent content = new ClipboardContent();
        content.putString(machineIdField.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML
    private void handleActivate() {
        FormValidator validator = new FormValidator().required(keyField, "license.key");
        if (!validator.validate(keyField.getScene().getWindow())) {
            return;
        }
        String key = keyField.getText();
        FxAsync.runAction(activateButton, () -> licenseService.activate(key), () -> {
            Dialogs.info(keyField.getScene().getWindow(), I18n.get("license.activated"));
            keyField.clear();
            refresh();
            if (onActivated != null) {
                onActivated.run();
            }
        });
    }
}
