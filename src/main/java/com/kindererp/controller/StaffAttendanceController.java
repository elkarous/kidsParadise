package com.kindererp.controller;

import com.kindererp.service.StaffKind;
import com.kindererp.util.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Tab;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** Date selector plus one attendance sheet per staff kind. */
@Component
@Scope("prototype")
public class StaffAttendanceController {

    @FXML private DatePicker datePicker;
    @FXML private Tab teachersTab;
    @FXML private Tab employeesTab;

    @FXML
    private void initialize() {
        StaffAttendanceListController teachers = sheet(teachersTab, StaffKind.TEACHER);
        StaffAttendanceListController employees = sheet(employeesTab, StaffKind.EMPLOYEE);
        datePicker.valueProperty().addListener((obs, old, date) -> List.of(teachers, employees).forEach(s -> s.setDate(date)));
        datePicker.setValue(LocalDate.now());
    }

    private static StaffAttendanceListController sheet(Tab tab, StaffKind kind) {
        ViewLoader.View<StaffAttendanceListController> view = ViewLoader.load("staff_attendance_list");
        view.controller().setKind(kind);
        tab.setContent(view.root());
        return view.controller();
    }
}
