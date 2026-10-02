package com.kindererp.controller;

import com.kindererp.model.AttendanceStatus;
import com.kindererp.model.SchoolClass;
import com.kindererp.model.StudentAttendance;
import com.kindererp.service.AttendanceService;
import com.kindererp.service.ClassService;
import com.kindererp.util.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;

/** Daily attendance register of one class. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final ClassService classService;

    @FXML private ComboBox<SchoolClass> classCombo;
    @FXML private DatePicker datePicker;
    @FXML private Button saveButton;
    @FXML private TableView<StudentAttendance> table;
    @FXML private TableColumn<StudentAttendance, String> studentColumn;
    @FXML private TableColumn<StudentAttendance, StudentAttendance> statusColumn;
    @FXML private TableColumn<StudentAttendance, StudentAttendance> notesColumn;
    @FXML private Label summaryLabel;

    private final ObservableList<StudentAttendance> register = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        Tables.text(studentColumn, a -> a.getStudent().getLastName() + " " + a.getStudent().getFirstName());
        statusColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        statusColumn.setCellFactory(c -> new StatusCell());
        notesColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        notesColumn.setCellFactory(c -> new NotesCell());
        Tables.placeholder(table, "attendance.chooseClass");
        table.setItems(register);

        classCombo.setConverter(ComboBoxes.converter(c -> c.getLevel().getName() + " — " + c.getName()));
        datePicker.setValue(LocalDate.now());
        classCombo.valueProperty().addListener((obs, old, value) -> load());
        datePicker.valueProperty().addListener((obs, old, value) -> load());

        FxAsync.run(table, classService::classes, classes -> classCombo.getItems().setAll(classes));
    }

    private void load() {
        SchoolClass schoolClass = classCombo.getValue();
        LocalDate date = datePicker.getValue();
        if (schoolClass == null || date == null) {
            register.clear();
            updateSummary();
            return;
        }
        FxAsync.run(table, () -> attendanceService.getRegister(schoolClass.getId(), date), lines -> {
            register.setAll(lines);
            updateSummary();
        });
    }

    @FXML
    private void handleAllPresent() {
        register.forEach(line -> line.setStatus(AttendanceStatus.PRESENT));
        table.refresh();
        updateSummary();
    }

    @FXML
    private void handleSave() {
        if (register.isEmpty()) {
            return;
        }
        FormValidator validator = new FormValidator().notInFuture(datePicker, "attendance.date");
        if (!validator.validate(table.getScene().getWindow())) {
            return;
        }
        var lines = new ArrayList<>(register);
        FxAsync.runAction(saveButton, () -> attendanceService.saveRegister(lines), () -> {
            Dialogs.info(table.getScene().getWindow(), I18n.get("attendance.saved"));
            load();
        });
    }

    private void updateSummary() {
        long absent = register.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        summaryLabel.setText(I18n.get("attendance.summary", register.size(), register.size() - absent, absent));
    }

    private class StatusCell extends TableCell<StudentAttendance, StudentAttendance> {
        private final ComboBox<AttendanceStatus> combo = new ComboBox<>(FXCollections.observableArrayList(AttendanceStatus.values()));

        StatusCell() {
            combo.setConverter(ComboBoxes.enumConverter("attendanceStatus"));
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.valueProperty().addListener((obs, old, status) -> {
                if (getItem() != null && status != null && status != getItem().getStatus()) {
                    getItem().setStatus(status);
                    updateSummary();
                }
            });
        }

        @Override
        protected void updateItem(StudentAttendance item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                combo.setValue(item.getStatus());
                setGraphic(combo);
            }
        }
    }

    private static class NotesCell extends TableCell<StudentAttendance, StudentAttendance> {
        private final TextField field = new TextField();

        NotesCell() {
            field.textProperty().addListener((obs, old, text) -> {
                if (getItem() != null) {
                    getItem().setNotes(text);
                }
            });
        }

        @Override
        protected void updateItem(StudentAttendance item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                field.setText(item.getNotes());
                setGraphic(field);
            }
        }
    }
}
