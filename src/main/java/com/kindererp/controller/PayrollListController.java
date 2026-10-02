package com.kindererp.controller;

import com.kindererp.model.PaymentMethod;
import com.kindererp.model.SalaryType;
import com.kindererp.model.WorkingMonth;
import com.kindererp.service.PayrollService;
import com.kindererp.service.StaffKind;
import com.kindererp.service.dto.PayrollRow;
import com.kindererp.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Salaries of one staff kind for the selected month, and the payout form. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class PayrollListController {

    private final PayrollService payrollService;

    @FXML private TableView<PayrollRow> table;
    @FXML private TableColumn<PayrollRow, String> nameColumn;
    @FXML private TableColumn<PayrollRow, String> workColumn;
    @FXML private TableColumn<PayrollRow, String> grossColumn;
    @FXML private TableColumn<PayrollRow, String> paidColumn;
    @FXML private TableColumn<PayrollRow, String> pendingColumn;
    @FXML private TableColumn<PayrollRow, PayrollRow> statusColumn;
    @FXML private Label selectedLabel;
    @FXML private Label grossLabel;
    @FXML private Label paidLabel;
    @FXML private Label pendingLabel;
    @FXML private TextField amountField;
    @FXML private ComboBox<PaymentMethod> methodCombo;
    @FXML private TextField referenceField;
    @FXML private CheckBox printCheck;
    @FXML private Button payButton;

    private final ObservableList<PayrollRow> rows = FXCollections.observableArrayList();
    private StaffKind kind;
    private WorkingMonth month;

    @FXML
    private void initialize() {
        Tables.text(nameColumn, PayrollRow::name);
        Tables.amount(workColumn, r -> r.salaryType() == SalaryType.PER_SESSION
                ? I18n.get("payroll.sessionsCount", r.sessions())
                : I18n.get("payroll.absencesCount", r.absences()));
        Tables.amount(grossColumn, r -> Formats.money(r.gross()));
        Tables.amount(paidColumn, r -> Formats.money(r.paid()));
        Tables.amount(pendingColumn, r -> Formats.money(r.pending()));
        Tables.badge(statusColumn,
                r -> I18n.get(r.fullyPaid() ? "payroll.status.paid" : r.partiallyPaid() ? "payroll.status.partial" : "payroll.status.unpaid"),
                r -> r.fullyPaid() ? "badge-success" : r.partiallyPaid() ? "badge-warning" : "badge-danger");
        Tables.placeholder(table, "table.empty");
        table.setItems(rows);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> showSelection(row));

        methodCombo.getItems().setAll(PaymentMethod.values());
        methodCombo.setConverter(ComboBoxes.enumConverter("paymentMethod"));
        methodCombo.setValue(PaymentMethod.CASH);
        showSelection(null);
    }

    public void setKind(StaffKind kind) {
        this.kind = kind;
    }

    public void setMonth(WorkingMonth month) {
        this.month = month;
        load(null);
    }

    private void load(Long reselectStaffId) {
        if (month == null) {
            rows.clear();
            return;
        }
        FxAsync.run(table, () -> payrollService.monthPayroll(kind, month.getId()), list -> {
            rows.setAll(list);
            if (reselectStaffId != null) {
                list.stream().filter(r -> r.staffId().equals(reselectStaffId)).findFirst()
                        .ifPresent(r -> table.getSelectionModel().select(r));
            }
        });
    }

    private void showSelection(PayrollRow row) {
        boolean selected = row != null;
        selectedLabel.setText(selected ? row.name() : I18n.get("payroll.selectSomeone"));
        grossLabel.setText(selected ? Formats.money(row.gross()) : "");
        paidLabel.setText(selected ? Formats.money(row.paid()) : "");
        pendingLabel.setText(selected ? Formats.money(row.pending()) : "");
        amountField.setText(selected ? Formats.editableAmount(row.pending()) : "");
        payButton.setDisable(!selected || row.fullyPaid() || (month != null && month.isClosed()));
    }

    @FXML
    private void handlePay() {
        PayrollRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            return;
        }
        FormValidator validator = new FormValidator();
        BigDecimal amount = validator.amount(amountField, "payroll.amountNow", true);
        validator.check(amountField, amount == null || amount.compareTo(row.pending()) <= 0,
                "payroll.error.exceedsPendingField", "payroll.amountNow");
        if (!validator.validate(table.getScene().getWindow())) {
            return;
        }
        if (!Dialogs.confirm(table.getScene().getWindow(),
                I18n.get("payroll.confirm.question", Formats.money(amount), row.name(), Formats.month(month)))) {
            return;
        }
        WorkingMonth payMonth = month;
        FxAsync.run(payButton,
                () -> payrollService.pay(kind, row.staffId(), payMonth.getId(), amount, methodCombo.getValue(), referenceField.getText()),
                payment -> {
                    referenceField.clear();
                    if (printCheck.isSelected()) {
                        PayrollRow after = new PayrollRow(row.staffId(), row.name(), row.position(), row.salaryType(),
                                row.absences(), row.sessions(), row.gross(), row.paid().add(amount),
                                row.pending().subtract(amount));
                        PrintSupport.printPaySlip(table.getScene().getWindow(), after, payMonth, amount);
                    }
                    load(row.staffId());
                });
    }
}
