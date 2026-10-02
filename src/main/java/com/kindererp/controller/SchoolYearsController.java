package com.kindererp.controller;

import com.kindererp.model.SchoolYear;
import com.kindererp.model.WorkingMonth;
import com.kindererp.service.SchoolYearService;
import com.kindererp.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Create school years (with their months) and close / reopen months. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class SchoolYearsController {

    private final SchoolYearService schoolYearService;

    @FXML private TextField yearNameField;
    @FXML private Button createButton;
    @FXML private TableView<SchoolYear> yearTable;
    @FXML private TableColumn<SchoolYear, String> yearNameColumn;
    @FXML private TableColumn<SchoolYear, String> yearStartColumn;
    @FXML private TableColumn<SchoolYear, String> yearEndColumn;
    @FXML private TableView<WorkingMonth> monthTable;
    @FXML private TableColumn<WorkingMonth, String> monthNameColumn;
    @FXML private TableColumn<WorkingMonth, WorkingMonth> monthStatusColumn;
    @FXML private Button toggleMonthButton;

    @FXML
    private void initialize() {
        Tables.text(yearNameColumn, SchoolYear::getName);
        Tables.text(yearStartColumn, y -> Formats.date(y.getStartDate()));
        Tables.text(yearEndColumn, y -> Formats.date(y.getEndDate()));
        Tables.text(monthNameColumn, m -> Formats.monthName(m.getCalendarMonth()) + " " + m.getCalendarYear());
        Tables.badge(monthStatusColumn, m -> I18n.get(m.isClosed() ? "month.closed" : "month.open"),
                m -> m.isClosed() ? "badge-neutral" : "badge-success");
        Tables.placeholder(monthTable, "schoolYears.selectYear");
        yearTable.getSelectionModel().selectedItemProperty().addListener((obs, old, year) -> loadMonths(year));
        monthTable.getSelectionModel().selectedItemProperty().addListener((obs, old, month) -> {
            toggleMonthButton.setDisable(month == null);
            if (month != null) {
                toggleMonthButton.setText(I18n.get(month.isClosed() ? "schoolYears.reopenMonth" : "schoolYears.closeMonth"));
            }
        });
        toggleMonthButton.setDisable(true);
        loadYears();
    }

    private void loadYears() {
        FxAsync.run(yearTable, schoolYearService::findAll, years -> {
            yearTable.getItems().setAll(years);
            yearNameField.setText(nextYearName(years));
            if (!years.isEmpty()) {
                yearTable.getSelectionModel().select(years.stream()
                        .filter(y -> AppState.currentSchoolYear() != null && y.equals(AppState.currentSchoolYear()))
                        .findFirst().orElse(years.get(0)));
            }
        });
    }

    /** The year after the most recent one, or the current one when none exists. */
    private static String nextYearName(java.util.List<SchoolYear> years) {
        if (years.isEmpty()) {
            return SchoolYearService.suggestedName(AppState.settings().getYearStartMonth());
        }
        int start = years.get(0).getStartDate().getYear() + 1;
        return start + "-" + (start + 1);
    }

    private void loadMonths(SchoolYear year) {
        if (year == null) {
            monthTable.getItems().clear();
            return;
        }
        FxAsync.run(monthTable, () -> schoolYearService.months(year.getId()), months -> monthTable.getItems().setAll(months));
    }

    @FXML
    private void handleCreate() {
        FormValidator validator = new FormValidator().required(yearNameField, "schoolYears.name");
        if (!validator.validate(yearTable.getScene().getWindow())) {
            return;
        }
        String name = yearNameField.getText();
        FxAsync.run(createButton, () -> schoolYearService.createYear(name), year -> {
            Dialogs.info(yearTable.getScene().getWindow(), I18n.get("schoolYears.created", year.getName()));
            loadYears();
        });
    }

    @FXML
    private void handleToggleMonth() {
        WorkingMonth month = monthTable.getSelectionModel().getSelectedItem();
        if (month == null) {
            return;
        }
        FxAsync.runAction(toggleMonthButton, () -> schoolYearService.setMonthClosed(month.getId(), !month.isClosed()),
                () -> loadMonths(yearTable.getSelectionModel().getSelectedItem()));
    }
}
