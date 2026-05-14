package com.kids.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class LoginController {

    private final ConfigurableApplicationContext springContext; // Inject the context

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;

    @FXML
    public void handleLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        if ("admin".equals(user) && "admin123".equals(pass)) {

                // 1. Load the new FXML
                navigateToDashboard();

    }}

    private void navigateToDashboard() {
        try {
            // Force the app to look for Arabic specifically
            Locale locale = new Locale("ar");
            ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
            URL fxmlLocation = getClass().getResource("/fxml/student_management.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlLocation);

            // 2. IMPORTANT: Pass the bundle to the loader
            loader.setResources(bundle);

            // 3. Keep your Spring context factory
            loader.setControllerFactory(springContext::getBean);

            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}