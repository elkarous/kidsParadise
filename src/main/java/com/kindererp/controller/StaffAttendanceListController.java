package com.kindererp.controller;

import com.kindererp.model.AttendanceStatus;
import com.kindererp.service.StaffAttendanceService;
import com.kindererp.service.StaffKind;
import com.kindererp.service.dto.StaffAttendanceRow;
import com.kindererp.util.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** One day of attendance for teachers or employees: status, arrival/departure, sessions and notes. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class StaffAttendanceListController {

    private final StaffAttendanceService attendanceService;

    @FXML private TableView<StaffAttendanceRow> table;
    @FXML private TableColumn<StaffAttendanceRow, String> nameColumn;
    @FXML private TableColumn<StaffAttendanceRow, StaffAttendanceRow> statusColumn;
    @FXML private TableColumn<StaffAttendanceRow, StaffAttendanceRow> checkInColumn;
    @FXML private TableColumn<StaffAttendanceRow, StaffAttendanceRow> checkOutColumn;
    @FXML private TableColumn<StaffAttendanceRow, StaffAttendanceRow> sessionsColumn;
    @FXML private TableColumn<StaffAttendanceRow, StaffAttendanceRow> notesColumn;
    @FXML private Button saveButton;

    private final ObservableList<StaffAttendanceRow> rows = FXCollections.observableArrayList();
    private StaffKind kind;
    private LocalDate date;

    @FXML
    private void initialize() {
        Tables.text(nameColumn, StaffAttendanceRow::getName);
        editable(statusColumn, this::statusEditor);
        editable(checkInColumn, row -> timeEditor(row.getCheckIn(), row, StaffAttendanceRow::setCheckIn));
        editable(checkOutColumn, row -> timeEditor(row.getCheckOut(), row, StaffAttendanceRow::setCheckOut));
        editable(sessionsColumn, this::sessionsEditor);
        editable(notesColumn, this::notesEditor);
        Tables.placeholder(table, "table.empty");
        table.setItems(rows);
    }

    public void setKind(StaffKind kind) {
        this.kind = kind;
    }

    public void setDate(LocalDate date) {
        this.date = date;
        if (date == null) {
            rows.clear();
            return;
        }
        FxAsync.run(table, () -> attendanceService.day(kind, date), rows::setAll);
    }

    @FXML
    private void handleSave() {
        if (date == null || rows.isEmpty()) {
            return;
        }
        FormValidator validator = new FormValidator().rule(!date.isAfter(LocalDate.now()), "attendance.error.futureDate");
        for (StaffAttendanceRow row : rows) {
            validator.rule(row.getCheckIn() == null || row.getCheckOut() == null || !row.getCheckOut().isBefore(row.getCheckIn()),
                    "attendance.error.timeOrder", row.getName());
        }
        if (!validator.validate(table.getScene().getWindow())) {
            return;
        }
        var snapshot = new ArrayList<>(rows);
        FxAsync.runAction(saveButton, () -> attendanceService.saveDay(kind, date, snapshot),
                () -> Dialogs.info(table.getScene().getWindow(), I18n.get("attendance.saved")));
    }

    private Node statusEditor(StaffAttendanceRow row) {
        ComboBox<AttendanceStatus> combo = new ComboBox<>(FXCollections.observableArrayList(AttendanceStatus.values()));
        combo.setConverter(ComboBoxes.enumConverter("attendanceStatus"));
        combo.setValue(row.getStatus());
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.valueProperty().addListener((obs, old, status) -> {
            row.setStatus(status);
            table.refresh();
        });
        return combo;
    }

    private Node timeEditor(LocalTime value, StaffAttendanceRow row, BiConsumer<StaffAttendanceRow, LocalTime> setter) {
        TextField field = new TextField(Formats.time(value));
        field.setPromptText("HH:mm");
        field.setDisable(!row.getStatus().isPresent());
        field.textProperty().addListener((obs, old, text) -> {
            LocalTime time = Formats.parseTime(text);
            boolean valid = text.isBlank() || time != null;
            field.getStyleClass().remove("field-error");
            if (!valid) {
                field.getStyleClass().add("field-error");
            }
            setter.accept(row, time);
        });
        return field;
    }

    private Node sessionsEditor(StaffAttendanceRow row) {
        Spinner<Integer> spinner = new Spinner<>(0, 12, row.getSessions());
        spinner.setDisable(!row.getStatus().isPresent());
        spinner.setPrefWidth(80);
        spinner.valueProperty().addListener((obs, old, value) -> row.setSessions(value));
        return spinner;
    }

    private Node notesEditor(StaffAttendanceRow row) {
        TextField field = new TextField(row.getNotes());
        field.textProperty().addListener((obs, old, text) -> row.setNotes(text));
        return field;
    }

    /** A column whose cell shows an editor built for the row (rebuilt when the row changes). */
    private static void editable(TableColumn<StaffAttendanceRow, StaffAttendanceRow> column,
                                 Function<StaffAttendanceRow, Node> editor) {
        column.setSortable(false);
        column.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        column.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(StaffAttendanceRow row, boolean empty) {
                super.updateItem(row, empty);
                setGraphic(empty || row == null ? null : editor.apply(row));
            }
        });
    }
}
