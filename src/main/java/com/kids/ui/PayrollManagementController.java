package com.kids.ui;

import com.kids.entities.Employee;
import com.kids.entities.PaymentMethod;
import com.kids.entities.Teacher;
import com.kids.entities.WorkingMonth;
import com.kids.repositories.WorkingMonthRepository;
import com.kids.services.EmployeeService;
import com.kids.services.TeacherService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class PayrollManagementController implements Initializable {

    private final TeacherService teacherService;
    private final EmployeeService employeeService;
    private final WorkingMonthRepository workingMonthRepository;

    @FXML private ComboBox<WorkingMonth> cbPayrollMonth;

    // ─── أعمدة جدول الأساتذة ──────────────────────────────────────
    @FXML private TableView<TeacherPayrollRow> teacherPayrollTable;
    @FXML private TableColumn<TeacherPayrollRow, String> colTeacherPayrollName;
    @FXML private TableColumn<TeacherPayrollRow, String> colTeacherGross;
    @FXML private TableColumn<TeacherPayrollRow, String> colTeacherPaid;
    @FXML private TableColumn<TeacherPayrollRow, String> colTeacherPending;
    @FXML private TableColumn<TeacherPayrollRow, TeacherPayrollRow> colTeacherStatus;
    @FXML private Label lblTeacherGross;
    @FXML private Label lblTeacherPaid;
    @FXML private Label lblTeacherPending;
    @FXML private TextField txtTeacherPayAmount;
    @FXML private ComboBox<PaymentMethod> cbTeacherPaymentMethod;

    // ─── أعمدة جدول الموظفين ──────────────────────────────────────
    @FXML private TableView<EmployeePayrollRow> employeePayrollTable;
    @FXML private TableColumn<EmployeePayrollRow, String> colEmployeePayrollName;
    @FXML private TableColumn<EmployeePayrollRow, String> colEmployeeGross;
    @FXML private TableColumn<EmployeePayrollRow, String> colEmployeePaid;
    @FXML private TableColumn<EmployeePayrollRow, String> colEmployeePending;
    @FXML private TableColumn<EmployeePayrollRow, EmployeePayrollRow> colEmployeeStatus;
    @FXML private Label lblEmployeeGross;
    @FXML private Label lblEmployeePaid;
    @FXML private Label lblEmployeePending;
    @FXML private TextField txtEmployeePayAmount;
    @FXML private ComboBox<PaymentMethod> cbEmployeePaymentMethod;

    private final ObservableList<TeacherPayrollRow> teacherPayrollList = FXCollections.observableArrayList();
    private final ObservableList<EmployeePayrollRow> employeePayrollList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initWorkingMonthFilter();
        initPaymentMethods();
        setupTableColumns();

        teacherPayrollTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateTeacherDetails(newV));
        employeePayrollTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateEmployeeDetails(newV));
        cbPayrollMonth.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> loadPayrollData());

        loadPayrollData();
    }

    private void initWorkingMonthFilter() {
        List<WorkingMonth> activeMonths = workingMonthRepository.findAll();
        cbPayrollMonth.setItems(FXCollections.observableArrayList(activeMonths));
        cbPayrollMonth.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(WorkingMonth item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getMonthName() + " " + item.getYear() + (item.isClosed() ? " (مغلق)" : ""));
            }
        });
        cbPayrollMonth.setButtonCell(cbPayrollMonth.getCellFactory().call(null));
        if (!cbPayrollMonth.getItems().isEmpty()) {
            cbPayrollMonth.setValue(cbPayrollMonth.getItems().get(cbPayrollMonth.getItems().size() - 1));
        }
    }

    private void initPaymentMethods() {
        cbTeacherPaymentMethod.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        cbTeacherPaymentMethod.setValue(PaymentMethod.CASH);
        cbEmployeePaymentMethod.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        cbEmployeePaymentMethod.setValue(PaymentMethod.CASH);
    }

    private void setupTableColumns() {
        // 🛠️ 1. ربط وإعداد أعمدة جدول الأساتذة
        colTeacherPayrollName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTeacher().getName()));
        colTeacherGross.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.3f DT", d.getValue().getGrossAmount())));
        colTeacherPaid.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.3f DT", d.getValue().getPaidAmount())));
        colTeacherPending.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.3f DT", d.getValue().getPendingAmount())));

        colTeacherStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colTeacherStatus.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(TeacherPayrollRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                setGraphic(createStatusBadge(item.getGrossAmount(), item.getPendingAmount()));
            }
        });

        // 🛠️ 2. ربط وإعداد أعمدة جدول الموظفين
        colEmployeePayrollName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmployee().getName()));
        colEmployeeGross.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.3f DT", d.getValue().getGrossAmount())));
        colEmployeePaid.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.3f DT", d.getValue().getPaidAmount())));
        colEmployeePending.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.3f DT", d.getValue().getPendingAmount())));

        colEmployeeStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
        colEmployeeStatus.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(EmployeePayrollRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                setGraphic(createStatusBadge(item.getGrossAmount(), item.getPendingAmount()));
            }
        });
    }

    private Label createStatusBadge(BigDecimal gross, BigDecimal pending) {
        Label badge = new Label();
        badge.setStyle("-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");

        if (pending.compareTo(BigDecimal.ZERO) == 0) {
            badge.setText("خالص بالكامل 🟢");
            badge.setStyle(badge.getStyle() + "-fx-background-color: #DEF7EC; -fx-text-fill: #03543F;");
        } else if (pending.compareTo(gross) < 0) {
            badge.setText("تسبيـق جزئي 🟡");
            badge.setStyle(badge.getStyle() + "-fx-background-color: #FEF08A; -fx-text-fill: #713F12;");
        } else {
            badge.setText("غير مدفوع 🔴");
            badge.setStyle(badge.getStyle() + "-fx-background-color: #FDE8E8; -fx-text-fill: #9B1C1C;");
        }
        return badge;
    }

    private void loadPayrollData() {
        WorkingMonth selectedWorkingMonth = cbPayrollMonth.getValue();
        if (selectedWorkingMonth == null) return;

        new Thread(() -> {
            // 🟢 1. حسابات الأساتذة باستخدام كائن الـ WorkingMonth مباشرة
            var teachers = teacherService.findAllActive();
            ObservableList<TeacherPayrollRow> tList = FXCollections.observableArrayList();
            for (Teacher t : teachers) {
                BigDecimal gross = teacherService.calculateGrossSalary(t.getId(), selectedWorkingMonth);
                BigDecimal pending = teacherService.calculatePendingSalary(t.getId(), selectedWorkingMonth);
                BigDecimal paid = gross.subtract(pending);
                tList.add(new TeacherPayrollRow(t, gross, paid, pending));
            }

            // 🟢 2. حسابات الموظفين باستخدام كائن الـ WorkingMonth مباشرة
            var employees = employeeService.findAll();
            ObservableList<EmployeePayrollRow> empList = FXCollections.observableArrayList();
            for (Employee emp : employees) {
                BigDecimal gross = employeeService.calculateGrossSalary(emp.getId(), selectedWorkingMonth);
                BigDecimal pending = employeeService.calculatePendingSalary(emp.getId(), selectedWorkingMonth);
                BigDecimal paid = gross.subtract(pending);
                empList.add(new EmployeePayrollRow(emp, gross, paid, pending));
            }

            Platform.runLater(() -> {
                teacherPayrollList.setAll(tList);
                teacherPayrollTable.setItems(teacherPayrollList);

                employeePayrollList.setAll(empList);
                employeePayrollTable.setItems(employeePayrollList);
            });
        }).start();
    }

    private void updateTeacherDetails(TeacherPayrollRow row) {
        if (row == null) {
            lblTeacherGross.setText("0.000 DT"); lblTeacherPaid.setText("0.000 DT"); lblTeacherPending.setText("0.000 DT");
            txtTeacherPayAmount.clear(); return;
        }
        lblTeacherGross.setText(String.format("%.3f DT", row.getGrossAmount()));
        lblTeacherPaid.setText(String.format("%.3f DT", row.getPaidAmount()));
        lblTeacherPending.setText(String.format("%.3f DT", row.getPendingAmount()));
        txtTeacherPayAmount.setText(String.format("%.3f", row.getPendingAmount()));
    }

    private void updateEmployeeDetails(EmployeePayrollRow row) {
        if (row == null) {
            lblEmployeeGross.setText("0.000 DT"); lblEmployeePaid.setText("0.000 DT"); lblEmployeePending.setText("0.000 DT");
            txtEmployeePayAmount.clear(); return;
        }
        lblEmployeeGross.setText(String.format("%.3f DT", row.getGrossAmount()));
        lblEmployeePaid.setText(String.format("%.3f DT", row.getPaidAmount()));
        lblEmployeePending.setText(String.format("%.3f DT", row.getPendingAmount()));
        txtEmployeePayAmount.setText(String.format("%.3f", row.getPendingAmount()));
    }

    @FXML
    void handlePayTeacher() {
        TeacherPayrollRow selected = teacherPayrollTable.getSelectionModel().getSelectedItem();
        WorkingMonth currentMonth = cbPayrollMonth.getValue();

        if (selected == null) { showAlert("تنبيه", "الرجاء اختيار أستاذ أولاً."); return; }
        if (selected.getPendingAmount().compareTo(BigDecimal.ZERO) == 0) { showAlert("منع صرف مكرر", "هذا الأستاذ خالص بالكامل لهذا الشهر بالفعل!"); return; }

        try {
            BigDecimal amountToPay = new BigDecimal(txtTeacherPayAmount.getText().trim());

            // 🟢 تمرير كائن الـ currentMonth مباشرة للـ Service دون تحويله لنص أو لـ YearMonth هنا
            teacherService.recordPayment(selected.getTeacher().getId(), currentMonth, amountToPay, cbTeacherPaymentMethod.getValue(), "SYS-PAID");
            showAlert("تمت العملية", "تم تسجيل صرف المستحقات بنجاح.");
            loadPayrollData();
        } catch (Exception e) { showAlert("خطأ", e.getMessage()); }
    }

    @FXML
    void handlePayEmployee() {
        EmployeePayrollRow selected = employeePayrollTable.getSelectionModel().getSelectedItem();
        WorkingMonth currentMonth = cbPayrollMonth.getValue();

        if (selected == null) { showAlert("تنبيه", "الرجاء اختيار موظف أولاً."); return; }
        if (selected.getPendingAmount().compareTo(BigDecimal.ZERO) == 0) { showAlert("منع صرف مكرر", "هذا الموظف خالص بالكامل لهذا الشهر الدراسي!"); return; }

        try {
            BigDecimal amountToPay = new BigDecimal(txtEmployeePayAmount.getText().trim());

            // 🟢 تمرير كائن الـ currentMonth مباشرة للـ Service
            employeeService.recordPayment(selected.getEmployee().getId(), currentMonth, amountToPay, cbEmployeePaymentMethod.getValue(), "SYS-PAID");
            showAlert("تمت العملية", "تم تسجيل الصرف وتحديث السجلات المالية.");
            loadPayrollData();
        } catch (Exception e) { showAlert("خطأ", e.getMessage()); }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
        alert.setTitle(title); alert.setHeaderText(null); alert.showAndWait();
    }

    @Data @AllArgsConstructor
    static class TeacherPayrollRow {
        private Teacher teacher;
        private BigDecimal grossAmount;
        private BigDecimal paidAmount;
        private BigDecimal pendingAmount;
    }

    @Data @AllArgsConstructor
    static class EmployeePayrollRow {
        private Employee employee;
        private BigDecimal grossAmount;
        private BigDecimal paidAmount;
        private BigDecimal pendingAmount;
    }
}