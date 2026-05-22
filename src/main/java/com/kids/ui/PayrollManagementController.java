package com.kids.ui;

import com.kids.entities.Employee;
import com.kids.entities.PaymentMethod;
import com.kids.entities.SalaryType;
import com.kids.entities.Teacher;
import com.kids.services.EmployeeService;
import com.kids.services.TeacherService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.YearMonth;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class PayrollManagementController implements Initializable {

    private final TeacherService teacherService;
    private final EmployeeService administratorService;
    private ResourceBundle bundle;

    // ── عناصر واجهة رواتب المعلمين (Teachers Payroll) ─────────────────
    @FXML private TableView<TeacherPayrollRow> teacherPayrollTable;
    @FXML private TableColumn<TeacherPayrollRow, String> colTeacherPayrollName;
    @FXML private TableColumn<TeacherPayrollRow, String> colTeacherSalaryType;
    @FXML private TableColumn<TeacherPayrollRow, String> colTeacherPending;

    @FXML private Label lblTeacherGross;
    @FXML private Label lblTeacherPending;
    @FXML private TextField txtTeacherPayAmount;
    @FXML private ComboBox<PaymentMethod> cbTeacherPaymentMethod;

    // ── عناصر واجهة رواتب الإداريين (Staff Payroll) ───────────────────
    @FXML private TableView<EmployeePayrollRow> employeePayrollTable;
    @FXML private TableColumn<EmployeePayrollRow, String> colEmployeePayrollName;
    @FXML private TableColumn<EmployeePayrollRow, String> colEmployeeRole;
    @FXML private TableColumn<EmployeePayrollRow, String> colEmployeePending;

    @FXML private Label lblEmployeeGross;
    @FXML private Label lblEmployeePending;
    @FXML private TextField txtEmployeePayAmount;
    @FXML private ComboBox<PaymentMethod> cbEmployeePaymentMethod;

    // القوائم المالية الذكية المحسوبة في الذاكرة
    private final ObservableList<TeacherPayrollRow> masterTeachersPayroll = FXCollections.observableArrayList();
    private final ObservableList<EmployeePayrollRow> masterEmployeesPayroll = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.bundle = rb;

        initTables();
        initDropdowns();
        setupSelectionListeners();
        loadPayrollData();
    }

    /**
     * تهيئة أعمدة الجداول وربطها بركائز البيانات المحسوبة
     */
    private void initTables() {
        // 1. جدول الأساتذة
        colTeacherPayrollName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTeacher().getName()));
        colTeacherSalaryType.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTeacher().getSalaryType() == SalaryType.FIXED_MONTHLY ? "شهري ثابت" : "بالحصة/الجلسة"
        ));
        colTeacherPending.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPending().toString() + " DT"));

        // 2. جدول الموظفين والإداريين
        colEmployeePayrollName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmployee().getName()));
        colEmployeeRole.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmployee().getRole()));
        colEmployeePending.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPending().toString() + " DT"));
    }

    /**
     * تعبئة خيارات طرق الدفع المتوفرة في اللوحات الجانبية
     */
    private void initDropdowns() {
        ObservableList<PaymentMethod> methods = FXCollections.observableArrayList(PaymentMethod.CASH,PaymentMethod.BANK_TRANSFER, PaymentMethod.CHEQUE);
        cbTeacherPaymentMethod.setItems(methods);
        cbTeacherPaymentMethod.getSelectionModel().selectFirst();

        cbEmployeePaymentMethod.setItems(methods);
        cbEmployeePaymentMethod.getSelectionModel().selectFirst();
    }

    /**
     * مستمعات تفاعلية لتحديث لوحة التحكم الجانبية فور النقر على سطر الموظف أو المعلم
     */
    private void setupSelectionListeners() {
        // عند اختيار معلم
        teacherPayrollTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblTeacherGross.setText(newVal.getGross().toString() + " DT");
                lblTeacherPending.setText(newVal.getPending().toString() + " DT");
                txtTeacherPayAmount.setText(newVal.getPending().toPlainString());
            }
        });

        // عند اختيار موظف إداري
        employeePayrollTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblEmployeeGross.setText(newVal.getGross().toString() + " DT");
                lblEmployeePending.setText(newVal.getPending().toString() + " DT");
                txtEmployeePayAmount.setText(newVal.getPending().toPlainString());
            }
        });
    }

    /**
     * جلب الحسابات المالية الحية من الـ Services للشهر الحالي في الخلفية (Multi-threading)
     */
    private void loadPayrollData() {
        YearMonth currentMonth = YearMonth.now();

        new Thread(() -> {
            // 1. حساب وتحميل مستحقات الأساتذة
            var activeTeachers = teacherService.findAllActive();
            ObservableList<TeacherPayrollRow> teacherRows = FXCollections.observableArrayList();
            for (Teacher t : activeTeachers) {
                BigDecimal gross = teacherService.calculateGrossSalary(t.getId(), currentMonth);
                BigDecimal pending = teacherService.calculatePendingSalary(t.getId(), currentMonth);
                teacherRows.add(new TeacherPayrollRow(t, gross, pending));
            }

            // 2. حساب وتحميل مستحقات موظفي الطاقم الإداري والعملة
            var activeEmployees = administratorService.findAll();
            ObservableList<EmployeePayrollRow> employeeRows = FXCollections.observableArrayList();
            for (Employee e : activeEmployees) {
                // الموظف راتبه شهري ثابت دائماً من حقل baseSalary
                BigDecimal gross = e.getBaseSalary();
                BigDecimal pending = administratorService.calculatePendingSalary(e.getId(), currentMonth);
                employeeRows.add(new EmployeePayrollRow(e, gross, pending));
            }

            // تحديث الواجهة على الـ UI Thread
            Platform.runLater(() -> {
                masterTeachersPayroll.setAll(teacherRows);
                teacherPayrollTable.setItems(masterTeachersPayroll);

                masterEmployeesPayroll.setAll(employeeRows);
                employeePayrollTable.setItems(masterEmployeesPayroll);
            });
        }).start();
    }

    /**
     * زر تأكيد صرف راتب المعلم (اللوحة اليمنى - التبويب الأول)
     */
    @FXML
    private void handlePayTeacher(ActionEvent event) {
        TeacherPayrollRow selected = teacherPayrollTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showNotify("تنبيه", "الرجاء تحديد معلم من الجدول لإتمام الصرف.");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(txtTeacherPayAmount.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("المبلغ يجب أن يكون أكبر من الصفر");

            teacherService.recordPayment(
                    selected.getTeacher().getId(),
                    YearMonth.now(),
                    amount,
                    cbTeacherPaymentMethod.getValue(),
                    "PAY-T-" + selected.getTeacher().getId()
            );

            showNotify("نجاح العملية", "تم تسجيل الصرف المالي بنجاح للمعلم: " + selected.getTeacher().getName());
            loadPayrollData(); // تحديث آلي للجداول والمستحقات المتبقية
            txtTeacherPayAmount.clear();
        } catch (Exception ex) {
            showNotify("خطأ", "فشلت العملية: " + ex.getMessage());
        }
    }

    /**
     * زر تأكيد صرف راتب الموظف الإداري (اللوحة اليمنى - التبويب الثاني)
     */
    @FXML
    private void handlePayEmployee(ActionEvent event) {
        EmployeePayrollRow selected = employeePayrollTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showNotify("تنبيه", "الرجاء تحديد موظف من الجدول لإتمام الصرف.");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(txtEmployeePayAmount.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("المبلغ يجب أن يكون أكبر من الصفر");

            administratorService.recordPayment(
                    selected.getEmployee().getId(),
                    YearMonth.now(),
                    amount,
                    cbEmployeePaymentMethod.getValue(),
                    "PAY-E-" + selected.getEmployee().getId()
            );

            showNotify("نجاح العملية", "تم تسجيل الصرف المالي بنجاح للموظف: " + selected.getEmployee().getName());
            loadPayrollData();
            txtEmployeePayAmount.clear();
        } catch (Exception ex) {
            showNotify("خطأ", "فشلت العملية: " + ex.getMessage());
        }
    }

    private void showNotify(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        alert.setTitle(title); alert.setHeaderText(null); alert.showAndWait();
    }

    // ── كائنات البيانات الداخلية (Wrappers) لفصل الحسابات في الجداول ────────────────

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TeacherPayrollRow {
        private Teacher teacher;
        private BigDecimal gross;
        private BigDecimal pending;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class EmployeePayrollRow {
        private Employee employee;
        private BigDecimal gross;
        private BigDecimal pending;
    }
}