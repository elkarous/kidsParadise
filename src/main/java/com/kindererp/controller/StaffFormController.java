package com.kindererp.controller;

import com.kindererp.model.SalaryType;
import com.kindererp.model.StaffMember;
import com.kindererp.model.StaffStatus;
import com.kindererp.service.StaffKind;
import com.kindererp.service.StaffService;
import com.kindererp.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Add / edit a teacher or an employee. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class StaffFormController {

    private final StaffService staffService;

    @FXML private TextField nameField;
    @FXML private Label positionLabel;
    @FXML private TextField positionField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private DatePicker hiringDatePicker;
    @FXML private ComboBox<StaffStatus> statusCombo;
    @FXML private ComboBox<SalaryType> salaryTypeCombo;
    @FXML private Label baseSalaryLabel;
    @FXML private TextField baseSalaryField;
    @FXML private Label penaltyLabel;
    @FXML private TextField penaltyField;
    @FXML private Button saveButton;

    private StaffMember member;
    @Getter
    private boolean saved;

    @FXML
    private void initialize() {
        statusCombo.getItems().setAll(StaffStatus.values());
        statusCombo.setConverter(ComboBoxes.enumConverter("staffStatus"));
        salaryTypeCombo.getItems().setAll(SalaryType.values());
        salaryTypeCombo.setConverter(ComboBoxes.enumConverter("salaryType"));
        salaryTypeCombo.valueProperty().addListener((obs, old, type) -> updateSalaryFields(type));
    }

    public void setMember(StaffKind kind, StaffMember member) {
        this.member = member;
        positionLabel.setText(I18n.get(kind == StaffKind.TEACHER ? "staff.specialty" : "staff.jobTitle"));
        nameField.setText(member.getName());
        positionField.setText(member.getPosition());
        phoneField.setText(member.getPhone());
        emailField.setText(member.getEmail());
        hiringDatePicker.setValue(member.getHiringDate() != null ? member.getHiringDate() : LocalDate.now());
        statusCombo.setValue(member.getStatus());
        salaryTypeCombo.setValue(member.getSalaryType());
        baseSalaryField.setText(Formats.editableAmount(member.getBaseSalary()));
        penaltyField.setText(Formats.editableAmount(member.getAbsencePenalty()));
        updateSalaryFields(member.getSalaryType());
    }

    private void updateSalaryFields(SalaryType type) {
        boolean fixed = type != SalaryType.PER_SESSION;
        baseSalaryLabel.setText(I18n.get(fixed ? "staff.monthlySalary" : "staff.sessionRate"));
        penaltyLabel.setVisible(fixed);
        penaltyField.setVisible(fixed);
    }

    @FXML
    private void handleSave() {
        FormValidator validator = new FormValidator()
                .required(nameField, "staff.name")
                .phone(phoneField, "staff.phone", false)
                .email(emailField, "staff.email")
                .required(hiringDatePicker, "staff.hiringDate")
                .required(statusCombo, "staff.status")
                .required(salaryTypeCombo, "staff.salaryType");
        BigDecimal baseSalary = validator.amount(baseSalaryField, "staff.baseSalary", false);
        boolean fixed = salaryTypeCombo.getValue() != SalaryType.PER_SESSION;
        BigDecimal penalty = fixed ? validator.amount(penaltyField, "staff.absencePenalty", false) : BigDecimal.ZERO;
        if (!validator.validate(saveButton.getScene().getWindow())) {
            return;
        }
        member.setName(nameField.getText());
        member.setPosition(positionField.getText());
        member.setPhone(phoneField.getText());
        member.setEmail(emailField.getText());
        member.setHiringDate(hiringDatePicker.getValue());
        member.setStatus(statusCombo.getValue());
        member.setSalaryType(salaryTypeCombo.getValue());
        member.setBaseSalary(baseSalary);
        member.setAbsencePenalty(penalty);
        FxAsync.run(saveButton, () -> staffService.save(member), result -> {
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
