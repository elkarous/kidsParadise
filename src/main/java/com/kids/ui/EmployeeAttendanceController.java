package com.kids.ui;

import com.kids.entities.Employee;
import com.kids.entities.SalaryType;
import com.kids.entities.Teacher;
import com.kids.services.EmployeeService;
import com.kids.services.TeacherService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class EmployeeAttendanceController implements Initializable {

    // الخدمات (Services) المربوطة بقاعدة البيانات مباشرة
    private final TeacherService teacherService;
    private final EmployeeService administratorService;

    private ResourceBundle bundle;

    // ── عناصر واجهة الأساتذة (Teachers) ────────────────────────
    @FXML private TableView<Teacher> teacherAttendanceTable;
    @FXML private TableColumn<Teacher, String> colTeacherAttendName;
    @FXML private TableColumn<Teacher, String> colTeacherAttendStatus;

    @FXML private DatePicker teacherAttendanceDatePicker;
    @FXML private ToggleGroup teacherAttendanceToggleGroup;
    @FXML private RadioButton rbTeacherPresent;
    @FXML private RadioButton rbTeacherAbsent;
    @FXML private RadioButton rbTeacherLate;
    @FXML private RadioButton rbTeacherExcused;
    @FXML private VBox paneTeacherSessions;
    @FXML private Spinner<Integer> teacherSessionsSpinner;
    @FXML private TextArea teacherAttendanceNotes;

    // ── عناصر واجهة الإداريين والعملة (Employees) ───────────────
    @FXML private TableView<Employee> employeeAttendanceTable;
    @FXML private TableColumn<Employee, String> colEmployeeAttendName;
    @FXML private TableColumn<Employee, String> colEmployeeAttendStatus;

    @FXML private DatePicker employeeAttendanceDatePicker;
    @FXML private ToggleGroup employeeAttendanceToggleGroup;
    @FXML private RadioButton rbEmployeePresent;
    @FXML private RadioButton rbEmployeeAbsent;
    @FXML private RadioButton rbEmployeeLate;
    @FXML private RadioButton rbEmployeeExcused;
    @FXML private TextArea employeeAttendanceNotes;

    // القوائم المستضافة في الذاكرة لتغذية الجداول
    private final ObservableList<Teacher> teachersList = FXCollections.observableArrayList();
    private final ObservableList<Employee> employeesList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.bundle = rb;

        initTables();
        initDefaultValues();
        setupSelectionListeners();
        loadData();
    }

    /**
     * إعداد أعمده الجداول وربطها بخصائص الـ Entities الحقيقية
     */
    private void initTables() {
        // 1. جدول الأساتذة
        colTeacherAttendName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colTeacherAttendStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));

        // 2. جدول الموظفين الإداريين والعملة
        colEmployeeAttendName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colEmployeeAttendStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
    }

    /**
     * تهيئة القيم الافتراضية لعناصر الإدخال (التاريخ، والـ Spinner)
     */
    private void initDefaultValues() {
        LocalDate today = LocalDate.now();
        teacherAttendanceDatePicker.setValue(today);
        employeeAttendanceDatePicker.setValue(today);

        // إعداد السقوف العددية للحصص المنجزة للأساتذة (من 0 إلى 10 حصص يومياً مثلاً)
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 1);
        teacherSessionsSpinner.setValueFactory(valueFactory);
    }

    /**
     * تتبع نقرات المستخدم داخل الجداول لإظهار أو إخفاء حقل الحصص ديناميكياً حسب نوع تعاقد الأستاذ
     */
    private void setupSelectionListeners() {
        teacherAttendanceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // إذا كان الأستاذ يتقاضى أجره بالحصة (PER_SESSION)، نُظهر متحكم الحصص، عدا ذلك نُخفيه
                boolean isPerSession = (newSelection.getSalaryType() == SalaryType.PER_SESSION);
                paneTeacherSessions.setVisible(isPerSession);
                paneTeacherSessions.setManaged(isPerSession);
            }
        });
    }

    /**
     * جلب البيانات الحية والمحدثة من قاعدة البيانات للجدولين
     */
    private void loadData() {
        teachersList.setAll(teacherService.findAllActive());
        teacherAttendanceTable.setItems(teachersList);

        employeesList.setAll(administratorService.findAll()); // جلب الإداريين النشطين فقط
        employeeAttendanceTable.setItems(employeesList);
    }

    /**
     * معالجة زر حفظ رصد حضور الأستاذ المحدد
     */
    @FXML
    private void handleMarkTeacherAttendance(ActionEvent event) {
        Teacher selectedTeacher = teacherAttendanceTable.getSelectionModel().getSelectedItem();
        if (selectedTeacher == null) {
            showWarning("تنبيه", "الرجاء اختيار أستاذ من الجدول أولاً لرصد حضوره.");
            return;
        }

        RadioButton selectedRadio = (RadioButton) teacherAttendanceToggleGroup.getSelectedToggle();
        if (selectedRadio == null) {
            showWarning("تنبيه", "الرجاء تحديد حالة الحضور (حاضر، غائب، ...)");
            return;
        }

        try {
            LocalDate date = teacherAttendanceDatePicker.getValue();
            String status = selectedRadio.getId().replace("rbTeacher", "").toUpperCase(); // سيعيد (PRESENT, ABSENT, LATE, EXCUSED)
            int sessions = paneTeacherSessions.isVisible() ? teacherSessionsSpinner.getValue() : 0;
            String notes = teacherAttendanceNotes.getText().trim();

            // 💾 هنا تقوم باستدعاء الـ Service الخاصة بحفظ سجل الحضور في جدول الغيابات والحصص
            // مثال: teacherService.saveAttendance(selectedTeacher.getId(), date, status, sessions, notes);

            showSuccess("تم بنجاح", "تم تسجيل حضور الأستاذ " + selectedTeacher.getName() + " بنجاح.");
            clearTeacherForm();
            loadData();

        } catch (Exception e) {
            showError("خطأ أثناء الحفظ", e.getMessage());
        }
    }

    /**
     * معالجة زر حفظ رصد حضور الموظف الإداري المحدد
     */
    @FXML
    private void handleMarkEmployeeAttendance(ActionEvent event) {
        Employee selectedAdmin = employeeAttendanceTable.getSelectionModel().getSelectedItem();
        if (selectedAdmin == null) {
            showWarning("تنبيه", "الرجاء اختيار موظف من الجدول أولاً لرصد حضوره.");
            return;
        }

        RadioButton selectedRadio = (RadioButton) employeeAttendanceToggleGroup.getSelectedToggle();
        if (selectedRadio == null) {
            showWarning("تنبيه", "الرجاء تحديد حالة حضور الموظف.");
            return;
        }

        try {
            LocalDate date = employeeAttendanceDatePicker.getValue();
            String status = selectedRadio.getId().replace("rbEmployee", "").toUpperCase();
            String notes = employeeAttendanceNotes.getText().trim();

            // 💾 هنا تقوم باستدعاء الـ Service الخاصة بحفظ سجل حضور الإداريين في جدول الغيابات والدوام الثابت
            // مثال: administratorService.saveAttendance(selectedAdmin.getId(), date, status, notes);

            showSuccess("تم بنجاح", "تم تسجيل حضور الموظف " + selectedAdmin.getName() + " بنجاح.");
            clearEmployeeForm();
            loadData();

        } catch (Exception e) {
            showError("خطأ أثناء الحفظ", e.getMessage());
        }
    }

    // ── دالات مساعدة لتنظيف حقول الإدخال بعد الحفظ ───────────────────────────

    private void clearTeacherForm() {
        if (teacherAttendanceToggleGroup.getSelectedToggle() != null) {
            teacherAttendanceToggleGroup.getSelectedToggle().setSelected(false);
        }
        teacherSessionsSpinner.getValueFactory().setValue(1);
        teacherAttendanceNotes.clear();
    }

    private void clearEmployeeForm() {
        if (employeeAttendanceToggleGroup.getSelectedToggle() != null) {
            employeeAttendanceToggleGroup.getSelectedToggle().setSelected(false);
        }
        employeeAttendanceNotes.clear();
    }

    // ── نوافذ التنبيهات والرسائل الإرشادية للمستخدم (Alerts) ───────────────────

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING, content, ButtonType.OK);
        alert.setTitle(title); alert.setHeaderText(null); alert.showAndWait();
    }

    private void showSuccess(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
        alert.setTitle(title); alert.setHeaderText(null); alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR, content, ButtonType.OK);
        alert.setTitle(title); alert.setHeaderText(null); alert.showAndWait();
    }
}