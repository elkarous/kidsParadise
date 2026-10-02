package com.kindererp.controller;

import com.kindererp.model.WorkingMonth;
import com.kindererp.service.SchoolYearService;
import com.kindererp.service.StaffKind;
import com.kindererp.util.AppState;
import com.kindererp.util.ComboBoxes;
import com.kindererp.util.Formats;
import com.kindererp.util.FxAsync;
import com.kindererp.util.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/** Month selector plus one payroll sheet per staff kind. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class PayrollController {

    private final SchoolYearService schoolYearService;

    @FXML private ComboBox<WorkingMonth> monthCombo;
    @FXML private Tab teachersTab;
    @FXML private Tab employeesTab;

    @FXML
    private void initialize() {
        PayrollListController teachers = sheet(teachersTab, StaffKind.TEACHER);
        PayrollListController employees = sheet(employeesTab, StaffKind.EMPLOYEE);
        monthCombo.setConverter(ComboBoxes.converter(Formats::month));
        monthCombo.valueProperty().addListener((obs, old, month) -> List.of(teachers, employees).forEach(s -> s.setMonth(month)));

        Long yearId = AppState.currentSchoolYear().getId();
        FxAsync.run(monthCombo, () -> schoolYearService.months(yearId), months -> {
            monthCombo.getItems().setAll(months);
            monthCombo.setValue(TuitionController.defaultMonth(months));
        });
    }

    private static PayrollListController sheet(Tab tab, StaffKind kind) {
        ViewLoader.View<PayrollListController> view = ViewLoader.load("payroll_list");
        view.controller().setKind(kind);
        tab.setContent(view.root());
        return view.controller();
    }
}
