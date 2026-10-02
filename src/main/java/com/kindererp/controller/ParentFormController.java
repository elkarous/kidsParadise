package com.kindererp.controller;

import com.kindererp.model.Parent;
import com.kindererp.service.ParentService;
import com.kindererp.util.FormValidator;
import com.kindererp.util.FxAsync;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ParentFormController {

    private final ParentService parentService;

    @FXML private TextField fatherField;
    @FXML private TextField motherField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Button saveButton;

    private Parent parent;
    @Getter
    private boolean saved;

    public void setParent(Parent parent) {
        this.parent = parent;
        fatherField.setText(parent.getFatherName());
        motherField.setText(parent.getMotherName());
        phoneField.setText(parent.getPhone());
        emailField.setText(parent.getEmail());
    }

    @FXML
    private void handleSave() {
        FormValidator validator = new FormValidator()
                .required(fatherField, "parent.fatherName")
                .phone(phoneField, "parent.phone", true)
                .email(emailField, "parent.email");
        if (!validator.validate(saveButton.getScene().getWindow())) {
            return;
        }
        parent.setFatherName(fatherField.getText());
        parent.setMotherName(motherField.getText());
        parent.setPhone(phoneField.getText());
        parent.setEmail(emailField.getText());
        FxAsync.run(saveButton, () -> parentService.save(parent), result -> {
            saved = true;
            close();
        });
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}
