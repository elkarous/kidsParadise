package com.kindererp.controller;

import com.kindererp.model.Level;
import com.kindererp.model.Parent;
import com.kindererp.model.SchoolClass;
import com.kindererp.model.Student;
import com.kindererp.service.ClassService;
import com.kindererp.service.ParentService;
import com.kindererp.service.StudentService;
import com.kindererp.util.ComboBoxes;
import com.kindererp.util.FormValidator;
import com.kindererp.util.FxAsync;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** Student registration / edit dialog. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class StudentFormController {

    private final StudentService studentService;
    private final ClassService classService;
    private final ParentService parentService;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private DatePicker birthDatePicker;
    @FXML private DatePicker enrollmentDatePicker;
    @FXML private ComboBox<Level> levelCombo;
    @FXML private ComboBox<SchoolClass> classCombo;
    @FXML private ComboBox<Parent> parentCombo;
    @FXML private Button saveButton;

    private Student student;
    private List<SchoolClass> allClasses = List.of();
    @Getter
    private boolean saved;

    @FXML
    private void initialize() {
        levelCombo.setConverter(ComboBoxes.converter(Level::getName));
        classCombo.setConverter(ComboBoxes.converter(SchoolClass::getName));
        levelCombo.valueProperty().addListener((obs, old, level) -> {
            classCombo.getItems().setAll(allClasses.stream()
                    .filter(c -> level == null || level.equals(c.getLevel())).toList());
            if (classCombo.getValue() != null && !classCombo.getItems().contains(classCombo.getValue())) {
                classCombo.setValue(null);
            }
        });
    }

    public void setStudent(Student student) {
        this.student = student;
        firstNameField.setText(student.getFirstName());
        lastNameField.setText(student.getLastName());
        birthDatePicker.setValue(student.getBirthDate());
        enrollmentDatePicker.setValue(student.getEnrollmentDate() != null ? student.getEnrollmentDate() : LocalDate.now());

        FxAsync.run(saveButton, () -> new Choices(classService.levels(), classService.classes(), parentService.findAll()), choices -> {
            allClasses = choices.classes();
            levelCombo.getItems().setAll(choices.levels());
            parentCombo.setValue(student.getParent());
            ComboBoxes.searchable(parentCombo, choices.parents(), p -> p.getFatherName() + " — " + p.getPhone());
            if (student.getSchoolClass() != null) {
                levelCombo.setValue(student.getSchoolClass().getLevel());
                classCombo.setValue(student.getSchoolClass());
            }
        });
    }

    @FXML
    private void handleSave() {
        FormValidator validator = new FormValidator()
                .required(firstNameField, "student.firstName")
                .required(lastNameField, "student.lastName")
                .required(birthDatePicker, "student.birthDate")
                .notInFuture(birthDatePicker, "student.birthDate")
                .required(classCombo, "class.name");
        if (!validator.validate(saveButton.getScene().getWindow())) {
            return;
        }
        student.setFirstName(firstNameField.getText());
        student.setLastName(lastNameField.getText());
        student.setBirthDate(birthDatePicker.getValue());
        student.setEnrollmentDate(enrollmentDatePicker.getValue());
        student.setSchoolClass(classCombo.getValue());
        student.setParent(parentCombo.getValue());
        FxAsync.run(saveButton, () -> studentService.save(student), result -> {
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

    private record Choices(List<Level> levels, List<SchoolClass> classes, List<Parent> parents) {
    }
}
