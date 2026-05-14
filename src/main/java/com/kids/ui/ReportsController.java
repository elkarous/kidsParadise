package com.kids.ui;

import com.kids.reports.ReportExportService;
import com.kids.services.FinancialReportingService;
import com.kids.services.FinancialReportingService.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.*;

/**
 * JavaFX Controller — Financial Reports Dashboard
 *
 * Layout:
 * ┌──────────────────────────────────────────────────────────┐
 * │  Month Selector  [Refresh]  [Export CSV]  [Export PDF]   │
 * ├──────────────────┬───────────────────────────────────────┤
 * │  Card A          │  Card B                               │
 * │  ┌────────────┐  │  ┌──────────────┐                    │
 * │  │  Income    │  │  │  Expenses    │                    │
 * │  │  مدخول     │  │  │  مصروف       │                    │
 * │  │ XXX.XXX DT │  │  │ XXX.XXX DT  │                    │
 * │  └────────────┘  │  └──────────────┘                    │
 * ├──────────────────┴───────────────────────────────────────┤
 * │  TabPane: [Student Revenue] [Teacher Payroll]            │
 * └──────────────────────────────────────────────────────────┘
 *
 * RTL is applied to the entire dashboard for Arabic locale.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportsController implements Initializable {

    private final FinancialReportingService reportingService;
    private final ReportExportService       exportService;
    private ResourceBundle bundle;

    // ── FXML ─────────────────────────────────────────────────────────────────

    @FXML private BorderPane rootPane;
    @FXML private Label      lblDashboardTitle;

    // Month selector (custom YearMonth spinner or ComboBox)
    @FXML private ComboBox<String> cbMonth;

    // Action buttons
    @FXML private Button btnRefresh;
    @FXML private Button btnExportCsv;
    @FXML private Button btnExportPdf;
    @FXML private Button btnPrint;

    // Card A — Income / مدخول
    @FXML private VBox   cardIncome;
    @FXML private Label  lblIncomeTitle;
    @FXML private Label  lblIncomeAmount;
    @FXML private Label  lblCollectionRate;

    // Card B — Expenses / مصروف
    @FXML private VBox   cardExpenses;
    @FXML private Label  lblExpensesTitle;
    @FXML private Label  lblExpensesAmount;
    @FXML private Label  lblPendingObligations;

    // Net profit strip
    @FXML private Label  lblNetProfit;

    // Tab: Student Revenue
    @FXML private TableView<StudentRevenueReport> studentRevenueTable;

    // Tab: Teacher Payroll
    @FXML private TableView<TeacherSalaryLine> teacherPayrollTable;
    @FXML private TableColumn<TeacherSalaryLine, String> colTeacherName;
    @FXML private TableColumn<TeacherSalaryLine, String> colSalaryType;
    @FXML private TableColumn<TeacherSalaryLine, String> colGross;
    @FXML private TableColumn<TeacherSalaryLine, String> colPaid;
    @FXML private TableColumn<TeacherSalaryLine, String> colPending;
    @FXML private TableColumn<TeacherSalaryLine, String> colStatus;

    private YearMonth selectedMonth = YearMonth.now();
    private TeacherPayrollReport lastPayrollReport;
    private StudentRevenueReport lastStudentReport;

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.bundle = rb;

        // ⭐ RTL for Arabic — applied to root so ALL children inherit
        if ("ar".equals(bundle.getLocale().getLanguage())) {
            rootPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        }

        applyLabels();
        setupMonthCombo();
        setupTeacherTable();
        setupButtons();
        refreshDashboard();
    }

    // ── Labels (bilingual) ────────────────────────────────────────────────────

    private void applyLabels() {
        lblDashboardTitle.setText(bundle.getString("nav.reports"));
        lblIncomeTitle.setText(bundle.getString("revenue"));    // "Revenue" / "مدخول"
        lblExpensesTitle.setText(bundle.getString("expenses")); // "Expenses" / "مصروف"
        btnRefresh.setText(bundle.getString("btn.refresh"));
        btnExportCsv.setText(bundle.getString("report.export.csv"));
        btnExportPdf.setText(bundle.getString("report.export.pdf"));
        btnPrint.setText(bundle.getString("report.print"));
    }

    // ── Month Selector ────────────────────────────────────────────────────────

    private void setupMonthCombo() {
        // Last 12 months
        ObservableList<String> months = FXCollections.observableArrayList();
        YearMonth current = YearMonth.now();
        for (int i = 0; i < 12; i++) {
            months.add(current.minusMonths(i).toString());
        }
        cbMonth.setItems(months);
        cbMonth.getSelectionModel().selectFirst();

        cbMonth.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, val) -> {
                if (val != null) {
                    selectedMonth = YearMonth.parse(val);
                    refreshDashboard();
                }
            }
        );
    }

    // ── Teacher Payroll Table ─────────────────────────────────────────────────

    private void setupTeacherTable() {
        colTeacherName.setText(bundle.getString("teacher.name"));
        colSalaryType.setText(bundle.getString("teacher.salary.type"));
        colGross.setText(bundle.getString("payroll.gross"));
        colPaid.setText(bundle.getString("payroll.paid"));
        colPending.setText(bundle.getString("salary.pending"));
        colStatus.setText(bundle.getString("teacher.status"));

        colTeacherName.setCellValueFactory(
            d -> new SimpleStringProperty(d.getValue().teacherName()));
        colSalaryType.setCellValueFactory(
            d -> new SimpleStringProperty(localizeType(d.getValue())));
        colGross.setCellValueFactory(
            d -> new SimpleStringProperty(format(d.getValue().grossSalary())));
        colPaid.setCellValueFactory(
            d -> new SimpleStringProperty(format(d.getValue().amountPaid())));
        colPending.setCellValueFactory(
            d -> new SimpleStringProperty(format(d.getValue().pendingAmount())));
        colStatus.setCellValueFactory(d -> {
            boolean paid = d.getValue().fullyPaid();
            String key = paid ? "teachers.paid" : "teachers.pending";
            return new SimpleStringProperty(bundle.getString(key));
        });

        // Highlight pending rows in amber
        teacherPayrollTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(TeacherSalaryLine item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (!item.fullyPaid()) {
                    setStyle("-fx-background-color: #FFF3CD;"); // amber warning
                } else {
                    setStyle("-fx-background-color: #D4EDDA;"); // green paid
                }
            }
        });
    }

    // ── Data Refresh ──────────────────────────────────────────────────────────

    @FXML
    private void refreshDashboard() {
        // Background thread for DB queries
        new Thread(() -> {
            DashboardSnapshot snapshot = reportingService.getDashboardSnapshot(selectedMonth);
            lastStudentReport  = reportingService.generateStudentReport(selectedMonth);
            lastPayrollReport  = reportingService.generateTeacherReport(selectedMonth);

            Platform.runLater(() -> updateUI(snapshot));
        }).start();
    }

    private void updateUI(DashboardSnapshot snapshot) {
        // Card A
        lblIncomeAmount.setText(format(snapshot.income()) + " DT");
        lblCollectionRate.setText(
            "%.1f%%".formatted(lastStudentReport.collectionRate()));

        // Card B
        lblExpensesAmount.setText(format(snapshot.expenses()) + " DT");
        lblPendingObligations.setText(
            bundle.getString("salary.pending") + ": " +
            format(lastPayrollReport.totalPending()) + " DT");

        // Net profit strip
        String netLabel = bundle.getString("net.profit") + ": " +
                          format(snapshot.netProfit()) + " DT";
        lblNetProfit.setText(netLabel);
        lblNetProfit.setStyle(
            snapshot.netProfit().compareTo(BigDecimal.ZERO) >= 0
                ? "-fx-text-fill: #155724; -fx-font-weight: bold;"
                : "-fx-text-fill: #721C24; -fx-font-weight: bold;"
        );

        // Teacher table
        teacherPayrollTable.setItems(
            FXCollections.observableArrayList(lastPayrollReport.lines()));
    }

    // ── Export Actions ────────────────────────────────────────────────────────

    private void setupButtons() {
        btnExportCsv.setOnAction(e -> handleExportCsv());
        btnExportPdf.setOnAction(e -> handleExportPdf());
        btnRefresh.setOnAction(e -> refreshDashboard());
        btnPrint.setOnAction(e -> handlePrint());
    }

    @FXML
    private void handleExportCsv() {
        if (lastPayrollReport == null || lastStudentReport == null) return;

        new Thread(() -> {
            try {
                exportService.exportTeacherPayrollCsv(
                    lastPayrollReport, Paths.get(System.getProperty("user.home")));
                exportService.exportStudentRevenueCsv(
                    lastStudentReport, Paths.get(System.getProperty("user.home")));

                Platform.runLater(() ->
                    showInfo("CSV exported to home directory."));
            } catch (IOException ex) {
                log.error("CSV export failed", ex);
                Platform.runLater(() -> showError(ex.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleExportPdf() {
        if (lastPayrollReport == null) return;

        new Thread(() -> {
            try {
                exportService.exportTeacherPayrollPdf(
                    lastPayrollReport, Paths.get(System.getProperty("user.home")));

                Platform.runLater(() -> showInfo("PDF exported."));
            } catch (Exception ex) {
                log.error("PDF export failed", ex);
                Platform.runLater(() -> showError(ex.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handlePrint() {
        if (lastPayrollReport == null) return;
        DashboardSnapshot snap = DashboardSnapshot.of(lastStudentReport, lastPayrollReport);
        String text = exportService.generateTextSummary(snap);

        // Show in a read-only dialog the user can print (Ctrl+P)
        TextArea ta = new TextArea(text);
        ta.setEditable(false);
        ta.setStyle("-fx-font-family: monospace; -fx-font-size: 13;");

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(bundle.getString("report.print"));
        dialog.getDialogPane().setContent(ta);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String format(BigDecimal value) {
        return value == null ? "—" : String.format("%,.3f", value);
    }

    private String localizeType(TeacherSalaryLine line) {
        return switch (line.salaryType()) {
            case FIXED_MONTHLY -> bundle.getString("salary.type.fixed");
            case PER_SESSION   -> bundle.getString("salary.type.per.session");
        };
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
