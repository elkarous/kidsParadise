package com.kids.ui;

import com.kids.entities.Administrator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

@Component
public class AdminManagerController implements Initializable {

    // === Tab 1: Admin Management Fields ===
    @FXML private TextField searchField;
    @FXML private TableView<Administrator> adminTable;
    @FXML private TableColumn<Administrator, String> colName;
    @FXML private TableColumn<Administrator, String> colRole;
    @FXML private TableColumn<Administrator, BigDecimal> colSalary;
    @FXML private TableColumn<Administrator, Administrator.AdminStatus> colStatus;

    @FXML private TextField txtAdminName;
    @FXML private TextField txtRole;
    @FXML private TextField txtSalary;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private DatePicker dpHiringDate;
    @FXML private ComboBox<Administrator.AdminStatus> cbStatusForm;

    // === Tab 2: Attendance Fields ===
    @FXML private TableView<Administrator> adminAttendanceTable;
    @FXML private TableColumn<Administrator, String> colAttendName;
    @FXML private TableColumn<Administrator, String> colAttendStatus;

    @FXML private DatePicker attendanceDatePicker;
    @FXML private ToggleGroup attendanceToggleGroup;
    @FXML private RadioButton rbPresent;
    @FXML private RadioButton rbAbsent;
    @FXML private RadioButton rbLate;
    @FXML private RadioButton rbExcused;
    @FXML private Spinner<Integer> sessionsSpinner;
    @FXML private TextArea attendanceNotes;
    @FXML private Button btnMarkAttendance;

    // === Tab 3: Payroll Fields ===
    @FXML private TableView<Administrator> adminPayrollTable;
    @FXML private TableColumn<Administrator, String> colPayrollName;
    @FXML private TableColumn<Administrator, BigDecimal> colPayrollPending;

    @FXML private Label lblGrossSalary;
    @FXML private Label lblPaid;
    @FXML private Label lblPending;

    @FXML private TextField txtPayAmount;
    @FXML private ComboBox<String> cbPaymentMethod;
    @FXML private TextField txtReference;
    @FXML private Button btnPayNow;

    // Lists for State Management
    private ObservableList<Administrator> masterAdminList = FXCollections.observableArrayList();
    private FilteredList<Administrator> filteredAdminList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTab1AdminCRUD();
        setupTab2Attendance();
        setupTab3Payroll();
        loadInitialData();
    }

    /**
     * تهيئة وإعداد التبويب الأول (إدارة الموظفين والعمليات)
     */
    private void setupTab1AdminCRUD() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("baseSalary"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        cbStatusForm.setItems(FXCollections.observableArrayList(Administrator.AdminStatus.values()));
        cbStatusForm.setValue(Administrator.AdminStatus.ACTIVE);
        dpHiringDate.setValue(LocalDate.now());

        // فلترة وعملية البحث التلقائي السريع
        filteredAdminList = new FilteredList<>(masterAdminList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredAdminList.setPredicate(admin -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return admin.getName().toLowerCase().contains(lowerCaseFilter) ||
                        admin.getRole().toLowerCase().contains(lowerCaseFilter);
            });
        });
        adminTable.setItems(filteredAdminList);

        // تعبئة الحقول تلقائياً عند الضغط على أي سطر في الجدول
        adminTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtAdminName.setText(newVal.getName());
                txtRole.setText(newVal.getRole());
                txtSalary.setText(newVal.getBaseSalary().toString());
                txtPhone.setText(newVal.getPhone());
                txtEmail.setText(newVal.getEmail());
                dpHiringDate.setValue(newVal.getHiringDate());
                cbStatusForm.setValue(newVal.getStatus());
            }
        });
    }

    /**
     * تهيئة تبويب الحضور والغياب
     */
    private void setupTab2Attendance() {
        colAttendName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAttendStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        adminAttendanceTable.setItems(masterAdminList);

        attendanceDatePicker.setValue(LocalDate.now());
        sessionsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 8)); // مثلاً من ساعة إلى 10 ساعات
    }

    /**
     * تهيئة تبويب الرواتب المالية
     */
    private void setupTab3Payroll() {
        colPayrollName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPayrollPending.setCellValueFactory(new PropertyValueFactory<>("baseSalary")); // مؤقتاً تعرض الراتب الأساسي
        adminPayrollTable.setItems(masterAdminList);

        cbPaymentMethod.setItems(FXCollections.observableArrayList("Cash / نقدي", "Bank Transfer / تحويل بنكي", "Check / شيك"));
        cbPaymentMethod.setValue("Cash / نقدي");

        adminPayrollTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblGrossSalary.setText(newVal.getBaseSalary().toString());
                lblPaid.setText("0.000"); // ستقوم بربطها بحسابات الدفع الفعلية لاحقاً
                lblPending.setText(newVal.getBaseSalary().toString());
                txtPayAmount.setText(newVal.getBaseSalary().toString());
            }
        });
    }

    private void loadInitialData() {
        // يمكنك هنا جلب البيانات من مستودع البيانات (Hibernate/Spring Data)
        // ومثال على ذلك إضافة كائنات تجريبية:
        masterAdminList.add(new Administrator(1L, "أحمد ياسين", "محاسب مالي", "059000000", "acc@school.com", LocalDate.now(), new BigDecimal("850.000"), Administrator.AdminStatus.ACTIVE));
        masterAdminList.add(new Administrator(2L, "منى سعيد", "مسؤولة استقبال", "059111111", "reception@school.com", LocalDate.now(), new BigDecimal("600.000"), Administrator.AdminStatus.ACTIVE));
    }

    // === الفعاليات البرمجية (Actions) لتبويب 1 ===
    @FXML
    private void handleAddAdmin() {
        if (validateFields()) {
            Administrator admin = Administrator.builder()
                    .name(txtAdminName.getText())
                    .role(txtRole.getText())
                    .baseSalary(new BigDecimal(txtSalary.getText()))
                    .phone(txtPhone.getText())
                    .email(txtEmail.getText())
                    .hiringDate(dpHiringDate.getValue())
                    .status(cbStatusForm.getValue())
                    .build();

            // adminService.save(admin);
            masterAdminList.add(admin);
            clearFields();
        }
    }

    @FXML
    private void handleUpdateAdmin() {
        Administrator selected = adminTable.getSelectionModel().getSelectedItem();
        if (selected != null && validateFields()) {
            selected.setName(txtAdminName.getText());
            selected.setRole(txtRole.getText());
            selected.setBaseSalary(new BigDecimal(txtSalary.getText()));
            selected.setPhone(txtPhone.getText());
            selected.setEmail(txtEmail.getText());
            selected.setHiringDate(dpHiringDate.getValue());
            selected.setStatus(cbStatusForm.getValue());

            // adminService.update(selected);
            adminTable.refresh();
            clearFields();
        }
    }

    @FXML
    private void handleDeleteAdmin() {
        Administrator selected = adminTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // adminService.delete(selected.getId());
            masterAdminList.remove(selected);
            clearFields();
        }
    }

    // === الفعاليات البرمجية (Actions) لتبويب 2 ===
    @FXML
    private void handleMarkAttendance() {
        Administrator selected = adminAttendanceTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            RadioButton selectedRadio = (RadioButton) attendanceToggleGroup.getSelectedToggle();
            String status = selectedRadio != null ? selectedRadio.getText() : "Present";
            int hoursOrSessions = sessionsSpinner.getValue();
            String notes = attendanceNotes.getText();

            // حفظ عملية تسجيل الحضور في قاعدة البيانات
            System.out.println("تسجيل حضور لـ: " + selected.getName() + " بـحالة " + status + " لعدد ساعات: " + hoursOrSessions);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText("تم تسجيل حضور الموظف بنجاح!");
            alert.showAndWait();
        }
    }

    // === الفعاليات البرمجية (Actions) لتبويب 3 ===
    @FXML
    private void handlePayNow() {
        Administrator selected = adminPayrollTable.getSelectionModel().getSelectedItem();
        if (selected != null && !txtPayAmount.getText().isEmpty()) {
            BigDecimal amount = new BigDecimal(txtPayAmount.getText());
            String method = cbPaymentMethod.getValue();
            String ref = txtReference.getText();

            // تنفيذ حركة الصرف المالي وحفظها
            System.out.println("تم صرف مبلغ: " + amount + " للموظف: " + selected.getName() + " بطريقة: " + method);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText("تم إتمام عملية صرف الراتب بنجاح وتوليد سند الصرف المالي!");
            alert.showAndWait();
        }
    }

    private void clearFields() {
        txtAdminName.clear();
        txtRole.clear();
        txtSalary.clear();
        txtPhone.clear();
        txtEmail.clear();
        dpHiringDate.setValue(LocalDate.now());
        cbStatusForm.setValue(Administrator.AdminStatus.ACTIVE);
        adminTable.getSelectionModel().clearSelection();
    }

    private boolean validateFields() {
        if (txtAdminName.getText().isEmpty() || txtRole.getText().isEmpty() || txtSalary.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("حقول ناقصة");
            alert.setContentText("الرجاء تعبئة الحقول الأساسية (الاسم، الوظيفة، الراتب).");
            alert.showAndWait();
            return false;
        }
        try {
            new BigDecimal(txtSalary.getText());
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("خطأ في البيانات");
            alert.setContentText("يرجى إدخال قيمة عددية صحيحة للراتب.");
            alert.showAndWait();
            return false;
        }
        return true;
    }
}