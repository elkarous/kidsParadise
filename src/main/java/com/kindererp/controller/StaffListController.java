package com.kindererp.controller;

import com.kindererp.model.SalaryType;
import com.kindererp.model.StaffMember;
import com.kindererp.model.StaffStatus;
import com.kindererp.service.StaffKind;
import com.kindererp.service.StaffService;
import com.kindererp.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** List of teachers or employees (depending on {@link #setKind}). */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class StaffListController {

    private final StaffService staffService;

    @FXML private TextField searchField;
    @FXML private ComboBox<SalaryType> salaryTypeFilter;
    @FXML private CheckBox showInactiveCheck;
    @FXML private Button addButton;
    @FXML private TableView<StaffMember> table;
    @FXML private TableColumn<StaffMember, String> nameColumn;
    @FXML private TableColumn<StaffMember, String> positionColumn;
    @FXML private TableColumn<StaffMember, String> phoneColumn;
    @FXML private TableColumn<StaffMember, String> salaryTypeColumn;
    @FXML private TableColumn<StaffMember, String> salaryColumn;
    @FXML private TableColumn<StaffMember, StaffMember> statusColumn;
    @FXML private TableColumn<StaffMember, Void> deleteColumn;

    private final ObservableList<StaffMember> members = FXCollections.observableArrayList();
    private final FilteredList<StaffMember> filtered = new FilteredList<>(members);
    private StaffKind kind;

    @FXML
    private void initialize() {
        Tables.text(nameColumn, StaffMember::getName);
        Tables.text(positionColumn, StaffMember::getPosition);
        Tables.text(phoneColumn, StaffMember::getPhone);
        Tables.text(salaryTypeColumn, m -> I18n.get("salaryType." + m.getSalaryType().name()));
        Tables.amount(salaryColumn, m -> Formats.money(m.getBaseSalary()));
        Tables.badge(statusColumn, m -> I18n.get("staffStatus." + m.getStatus().name()),
                m -> m.getStatus() == StaffStatus.ACTIVE ? "badge-success" : "badge-neutral");
        Tables.button(deleteColumn, Icons::trash, "btn.delete", true, this::delete);
        Tables.onDoubleClick(table, this::openForm);
        Tables.placeholder(table, "table.empty");
        table.setItems(filtered);

        List<SalaryType> types = new ArrayList<>();
        types.add(null);
        types.addAll(List.of(SalaryType.values()));
        salaryTypeFilter.getItems().setAll(types);
        ComboBoxes.withAllOption(salaryTypeFilter, t -> I18n.get("salaryType." + t.name()));

        searchField.textProperty().addListener((obs, old, text) -> applyFilter());
        salaryTypeFilter.valueProperty().addListener((obs, old, type) -> applyFilter());
        showInactiveCheck.selectedProperty().addListener((obs, old, show) -> applyFilter());
    }

    public void setKind(StaffKind kind) {
        this.kind = kind;
        positionColumn.setText(I18n.get(kind == StaffKind.TEACHER ? "staff.specialty" : "staff.jobTitle"));
        addButton.setText(I18n.get(kind == StaffKind.TEACHER ? "staff.addTeacher" : "staff.addEmployee"));
        load();
    }

    private void load() {
        FxAsync.run(table, () -> staffService.findAll(kind), list -> {
            members.setAll(list);
            applyFilter();
        });
    }

    private void applyFilter() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        SalaryType type = salaryTypeFilter.getValue();
        boolean showInactive = showInactiveCheck.isSelected();
        filtered.setPredicate(m -> (showInactive || m.getStatus() == StaffStatus.ACTIVE)
                && (type == null || type == m.getSalaryType())
                && (query.isEmpty() || (m.getName() + " " + Objects.toString(m.getPosition(), ""))
                .toLowerCase(Locale.ROOT).contains(query)));
    }

    @FXML
    private void handleAdd() {
        openForm(staffService.newMember(kind));
    }

    private void openForm(StaffMember member) {
        ViewLoader.Dialog<StaffFormController> dialog = ViewLoader.dialog("staff_form",
                member.getId() == null ? "staff.add" : "staff.edit", table.getScene().getWindow());
        dialog.controller().setMember(kind, member);
        dialog.stage().showAndWait();
        if (dialog.controller().isSaved()) {
            load();
        }
    }

    private void delete(StaffMember member) {
        if (Dialogs.confirm(table.getScene().getWindow(), I18n.get("staff.delete.confirm", member.getName()))) {
            FxAsync.runAction(table, () -> staffService.delete(kind, member.getId()), this::load);
        }
    }
}
