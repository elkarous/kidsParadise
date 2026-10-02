package com.kindererp.controller;

import com.kindererp.model.WorkingMonth;
import com.kindererp.service.PaymentService;
import com.kindererp.service.SchoolYearService;
import com.kindererp.service.dto.TuitionOverview;
import com.kindererp.service.dto.TuitionRow;
import com.kindererp.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/** Monthly tuition: who has paid, who has not, totals; register payments and print receipts. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class TuitionController {

    private final PaymentService paymentService;
    private final SchoolYearService schoolYearService;

    @FXML private ComboBox<WorkingMonth> monthCombo;
    @FXML private TextField searchField;
    @FXML private CheckBox unpaidOnlyCheck;
    @FXML private TableView<TuitionRow> table;
    @FXML private TableColumn<TuitionRow, String> parentColumn;
    @FXML private TableColumn<TuitionRow, String> phoneColumn;
    @FXML private TableColumn<TuitionRow, String> childrenColumn;
    @FXML private TableColumn<TuitionRow, String> amountColumn;
    @FXML private TableColumn<TuitionRow, TuitionRow> statusColumn;
    @FXML private TableColumn<TuitionRow, String> dateColumn;
    @FXML private TableColumn<TuitionRow, Void> actionColumn;
    @FXML private Label expectedLabel;
    @FXML private Label collectedLabel;
    @FXML private Label pendingLabel;

    private final ObservableList<TuitionRow> rows = FXCollections.observableArrayList();
    private final FilteredList<TuitionRow> filtered = new FilteredList<>(rows);

    @FXML
    private void initialize() {
        Tables.text(parentColumn, r -> r.parent().getFatherName());
        Tables.text(phoneColumn, r -> r.parent().getPhone());
        Tables.amount(childrenColumn, r -> String.valueOf(r.fee().childCount()));
        Tables.amount(amountColumn, r -> Formats.money(r.fee().total()));
        Tables.badge(statusColumn,
                r -> I18n.get(r.paid() ? "payment.status.paid" : "payment.status.unpaid"),
                r -> r.paid() ? "badge-success" : "badge-danger");
        Tables.text(dateColumn, r -> r.paid() ? Formats.date(r.payment().getPaymentDate()) : "");
        actionColumn.setSortable(false);
        actionColumn.setCellFactory(c -> new ActionCell());
        Tables.placeholder(table, "table.empty");
        table.setItems(filtered);

        monthCombo.setConverter(ComboBoxes.converter(Formats::month));
        monthCombo.valueProperty().addListener((obs, old, month) -> load());
        searchField.textProperty().addListener((obs, old, text) -> applyFilter());
        unpaidOnlyCheck.selectedProperty().addListener((obs, old, selected) -> applyFilter());

        Long yearId = AppState.currentSchoolYear().getId();
        FxAsync.run(table, () -> schoolYearService.months(yearId), months -> {
            monthCombo.getItems().setAll(months);
            monthCombo.setValue(defaultMonth(months));
        });
    }

    /** The current calendar month if it belongs to the year, otherwise the first open month. */
    static WorkingMonth defaultMonth(List<WorkingMonth> months) {
        YearMonth now = YearMonth.now();
        return months.stream().filter(m -> m.yearMonth().equals(now)).findFirst()
                .or(() -> months.stream().filter(m -> !m.isClosed()).findFirst())
                .orElse(months.isEmpty() ? null : months.get(0));
    }

    private void load() {
        WorkingMonth month = monthCombo.getValue();
        if (month == null) {
            rows.clear();
            return;
        }
        FxAsync.run(table, () -> paymentService.monthOverview(month.getId()), this::show);
    }

    private void show(TuitionOverview overview) {
        rows.setAll(overview.rows());
        expectedLabel.setText(Formats.money(overview.expected()));
        collectedLabel.setText(Formats.money(overview.collected()));
        pendingLabel.setText(Formats.money(overview.pending()));
        applyFilter();
    }

    private void applyFilter() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        boolean unpaidOnly = unpaidOnlyCheck.isSelected();
        filtered.setPredicate(r -> (!unpaidOnly || !r.paid())
                && (query.isEmpty() || (r.parent().getFatherName() + " " + r.parent().getPhone()).toLowerCase(Locale.ROOT).contains(query)));
    }

    private void pay(TuitionRow row) {
        ViewLoader.Dialog<PaymentDialogController> dialog =
                ViewLoader.dialog("payment_dialog", "payment.dialog.title", table.getScene().getWindow());
        dialog.controller().setData(row.parent(), monthCombo.getValue());
        dialog.stage().showAndWait();
        if (dialog.controller().getPayment() != null) {
            load();
        }
    }

    /** "Pay" for unpaid families, "Receipt" (print) for paid ones. */
    private class ActionCell extends TableCell<TuitionRow, Void> {
        private final Button button = new Button();

        ActionCell() {
            button.setOnAction(e -> {
                TuitionRow row = getTableView().getItems().get(getIndex());
                if (row.paid()) {
                    PrintSupport.printTuitionReceipt(table.getScene().getWindow(), row.payment());
                } else {
                    pay(row);
                }
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getIndex() >= getTableView().getItems().size()) {
                setGraphic(null);
                return;
            }
            TuitionRow row = getTableView().getItems().get(getIndex());
            button.getStyleClass().removeAll("btn-success", "btn-primary");
            if (row.paid()) {
                button.setText(I18n.get("btn.receipt"));
                button.setGraphic(Icons.print());
            } else {
                button.setText(I18n.get("btn.pay"));
                button.setGraphic(null);
                button.getStyleClass().add("btn-success");
            }
            button.setDisable(!row.paid() && monthCombo.getValue() != null && monthCombo.getValue().isClosed());
            setGraphic(new HBox(button));
        }
    }
}
