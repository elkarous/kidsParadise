package com.kids.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    public void handleLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        // For now, use hardcoded credentials.
        // Later, use: userRepository.findByUsername(user)
        if ("admin".equals(user) && "admin123".equals(pass)) {
            System.out.println("Login Successful!");
            // Logic to switch scene goes here
        } else {
            errorLabel.setText("Invalid username or password!");
        }
    }
}