package com.kindererp.controller;

import com.kindererp.util.Screens;
import javafx.application.Platform;
import javafx.fxml.FXML;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Activation screen shown when the trial is over: activate a key to continue, or quit. */
@Component
@Scope("prototype")
public class LicenseController {

    @FXML private AboutController aboutController;

    @FXML
    private void initialize() {
        aboutController.setOnActivated(Screens::showLogin);
    }

    @FXML
    private void handleQuit() {
        Platform.exit();
    }
}
