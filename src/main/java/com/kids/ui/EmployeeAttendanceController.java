package com.kids.ui;

import com.kids.entities.AttendanceStatus;
import com.kids.entities.Employee;
import com.kids.entities.Teacher;
import com.kids.services.EmployeeService;
import com.kids.services.TeacherService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class EmployeeAttendanceController implements Initializable {

    private final TeacherService teacherService;
    private final EmployeeService employeeService;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    // ─── عناصر واجهة الأساتذة ──────────────────────────────────────
    @FXML private DatePicker teacherAttendanceDatePicker;
    @FXML private TableView<TeacherAttendanceRow> teacherAttendanceTable;
    @FXML private TableColumn<TeacherAttendanceRow, String> colTeacherAttendName;
    @FXML private TableColumn<TeacherAttendanceRow, TeacherAttendanceRow> colTeacherActionStatus;
    @FXML private TableColumn<TeacherAttendanceRow, TeacherAttendanceRow> colTeacherCheckIn;
    @FXML private TableColumn<TeacherAttendanceRow, TeacherAttendanceRow> colTeacherCheckOut;
    @FXML private TableColumn<TeacherAttendanceRow, TeacherAttendanceRow> colTeacherNotes;

    // ─── عناصر واجهة الموظفين ──────────────────────────────────────
    @FXML private DatePicker employeeAttendanceDatePicker;
    @FXML private TableView<EmployeeAttendanceRow> employeeAttendanceTable;
    @FXML private TableColumn<EmployeeAttendanceRow, String> colEmployeeAttendName;
    @FXML private TableColumn<EmployeeAttendanceRow, EmployeeAttendanceRow> colEmployeeActionStatus;
    @FXML private TableColumn<EmployeeAttendanceRow, EmployeeAttendanceRow> colEmployeeCheckIn;
    @FXML private TableColumn<EmployeeAttendanceRow, EmployeeAttendanceRow> colEmployeeCheckOut;
    @FXML private TableColumn<EmployeeAttendanceRow, EmployeeAttendanceRow> colEmployeeNotes;

    private final ObservableList<TeacherAttendanceRow> teacherRows = FXCollections.observableArrayList();
    private final ObservableList<EmployeeAttendanceRow> employeeRows = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // تعيين تاريخ اليوم كقيمة افتراضية للواجهتين
        teacherAttendanceDatePicker.setValue(LocalDate.now());
        employeeAttendanceDatePicker.setValue(LocalDate.now());

        setupTeacherTableColumns();
        setupEmployeeTableColumns();

        loadTeachersAttendance();
        loadEmployeesAttendance();
    }

    // ─── 1. إعداد أعمدة جدول المعلمين تفاعلياً ─────────────────────────
    private void setupTeacherTableColumns() {
        colTeacherAttendName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTeacher().getName()));

        // بناء أزرار الخيارات (حاضر / غائب / متأخر) داخل السطر
        colTeacherActionStatus.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colTeacherActionStatus.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(TeacherAttendanceRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setGraphic(null); return; }

                RadioButton rbPresent = new RadioButton("حاضر");
                RadioButton rbAbsent = new RadioButton("غائب");
                RadioButton rbLate = new RadioButton("متأخر");

                ToggleGroup group = new ToggleGroup();
                rbPresent.setToggleGroup(group);
                rbAbsent.setToggleGroup(group);
                rbLate.setToggleGroup(group);

                // قراءة الحالة الحالية المخزنة في الذاكرة
                if (AttendanceStatus.PRESENT.equals(row.getStatus())) rbPresent.setSelected(true);
                else if (AttendanceStatus.ABSENT.equals(row.getStatus())) rbAbsent.setSelected(true);
                else if (AttendanceStatus.LATE.equals(row.getStatus())) rbLate.setSelected(true);

                // تحديث الكائن فوراً عند تغيير الاختيار بنقرة زر
                group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                    if (rbPresent.isSelected()) row.setStatus(AttendanceStatus.PRESENT);
                    else if (rbAbsent.isSelected()) row.setStatus(AttendanceStatus.ABSENT);
                    else if (rbLate.isSelected()) row.setStatus(AttendanceStatus.LATE);
                });

                setGraphic(new HBox(12, rbPresent, rbAbsent, rbLate));
            }
        });

        // عمود وقت الدخول للأستاذ
        colTeacherCheckIn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colTeacherCheckIn.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(TeacherAttendanceRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setGraphic(null); return; }

                TextField tf = new TextField(row.getCheckInTime() != null ? row.getCheckInTime().format(timeFormatter) : "08:00");
                tf.setPrefWidth(80);
                tf.setStyle("-fx-alignment: CENTER;");
                tf.textProperty().addListener((obs, oldV, newV) -> {
                    try { row.setCheckInTime(LocalTime.parse(newV, timeFormatter)); } catch (Exception e) { /* تجاهل صيغ الوقت غير المكتملة أثناء الكتابة */ }
                });
                setGraphic(tf);
            }
        });

        // عمود وقت الخروج للأستاذ
        colTeacherCheckOut.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colTeacherCheckOut.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(TeacherAttendanceRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setGraphic(null); return; }

                TextField tf = new TextField(row.getCheckOutTime() != null ? row.getCheckOutTime().format(timeFormatter) : "16:00");
                tf.setPrefWidth(80);
                tf.setStyle("-fx-alignment: CENTER;");
                tf.textProperty().addListener((obs, oldV, newV) -> {
                    try { row.setCheckOutTime(LocalTime.parse(newV, timeFormatter)); } catch (Exception e) { /* تجاهل صيغ الوقت غير المكتملة أثناء الكتابة */ }
                });
                setGraphic(tf);
            }
        });

        // حقل الملاحظات المدمج داخل السطر
        colTeacherNotes.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colTeacherNotes.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(TeacherAttendanceRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setGraphic(null); return; }

                TextField txtNotes = new TextField(row.getNotes());
                txtNotes.textProperty().addListener((obs, oldV, newV) -> row.setNotes(newV));
                setGraphic(txtNotes);
            }
        });
    }

    // ─── 2. إعداد أعمدة جدول الموظفين تفاعلياً ─────────────────────────
    private void setupEmployeeTableColumns() {
        colEmployeeAttendName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmployee().getName()));

        // بناء أزرار الخيارات (حاضر / غائب / متأخر) داخل سطر الموظف
        colEmployeeActionStatus.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colEmployeeActionStatus.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(EmployeeAttendanceRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setGraphic(null); return; }

                RadioButton rbPresent = new RadioButton("حاضر");
                RadioButton rbAbsent = new RadioButton("غائب");
                RadioButton rbLate = new RadioButton("متأخر");

                ToggleGroup group = new ToggleGroup();
                rbPresent.setToggleGroup(group);
                rbAbsent.setToggleGroup(group);
                rbLate.setToggleGroup(group);

                if (AttendanceStatus.PRESENT.equals(row.getStatus())) rbPresent.setSelected(true);
                else if (AttendanceStatus.ABSENT.equals(row.getStatus())) rbAbsent.setSelected(true);
                else if (AttendanceStatus.LATE.equals(row.getStatus())) rbLate.setSelected(true);

                group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                    if (rbPresent.isSelected()) row.setStatus(AttendanceStatus.PRESENT);
                    else if (rbAbsent.isSelected()) row.setStatus(AttendanceStatus.ABSENT);
                    else if (rbLate.isSelected()) row.setStatus(AttendanceStatus.LATE);
                });

                setGraphic(new HBox(12, rbPresent, rbAbsent, rbLate));
            }
        });

        // عمود وقت الدخول للموظف
        colEmployeeCheckIn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colEmployeeCheckIn.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(EmployeeAttendanceRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setGraphic(null); return; }

                TextField tf = new TextField(row.getCheckInTime() != null ? row.getCheckInTime().format(timeFormatter) : "08:00");
                tf.setPrefWidth(80);
                tf.setStyle("-fx-alignment: CENTER;");
                tf.textProperty().addListener((obs, oldV, newV) -> {
                    try { row.setCheckInTime(LocalTime.parse(newV, timeFormatter)); } catch (Exception e) { /* تجاهل الخطأ */ }
                });
                setGraphic(tf);
            }
        });

        // عمود وقت الخروج للموظف
        colEmployeeCheckOut.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colEmployeeCheckOut.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(EmployeeAttendanceRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setGraphic(null); return; }

                TextField tf = new TextField(row.getCheckOutTime() != null ? row.getCheckOutTime().format(timeFormatter) : "16:00");
                tf.setPrefWidth(80);
                tf.setStyle("-fx-alignment: CENTER;");
                tf.textProperty().addListener((obs, oldV, newV) -> {
                    try { row.setCheckOutTime(LocalTime.parse(newV, timeFormatter)); } catch (Exception e) { /* تجاهل الخطأ */ }
                });
                setGraphic(tf);
            }
        });

        // حقل الملاحظات المدمج داخل سطر الموظف
        colEmployeeNotes.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colEmployeeNotes.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(EmployeeAttendanceRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setGraphic(null); return; }

                TextField txtNotes = new TextField(row.getNotes());
                txtNotes.textProperty().addListener((obs, oldV, newV) -> row.setNotes(newV));
                setGraphic(txtNotes);
            }
        });
    }

    // ─── 3. دالات جلب البيانات ومزامنتها عند تغيير التاريخ ───────────────
    @FXML void handleTeacherDateChanged() { loadTeachersAttendance(); }
    @FXML void handleEmployeeDateChanged() { loadEmployeesAttendance(); }

    private void loadTeachersAttendance() {
        LocalDate selectedDate = teacherAttendanceDatePicker.getValue();
        new Thread(() -> {
            var teachers = teacherService.findAllActive();
            ObservableList<TeacherAttendanceRow> list = FXCollections.observableArrayList();
            for (Teacher t : teachers) {
                var savedRecord = teacherService.findAttendanceByDateAndId(t.getId(), selectedDate);
                if (savedRecord.isPresent()) {
                    list.add(new TeacherAttendanceRow(t, savedRecord.get().getStatus(), savedRecord.get().getCheckInTime(), savedRecord.get().getCheckOutTime(), savedRecord.get().getNotes()));
                } else {
                    // الافتراضي حاضر من الساعة 8 إلى 16 إذا لم يسجل مسبقاً
                    list.add(new TeacherAttendanceRow(t, AttendanceStatus.PRESENT, LocalTime.of(8, 0), LocalTime.of(16, 0), ""));
                }
            }
            Platform.runLater(() -> {
                teacherRows.setAll(list);
                teacherAttendanceTable.setItems(teacherRows);
            });
        }).start();
    }

    private void loadEmployeesAttendance() {
        LocalDate selectedDate = employeeAttendanceDatePicker.getValue();
        new Thread(() -> {
            var employees = employeeService.findAll();
            ObservableList<EmployeeAttendanceRow> list = FXCollections.observableArrayList();
            for (Employee emp : employees) {
                var savedRecord = employeeService.findAttendanceByDateAndId(emp.getId(), selectedDate);
                if (savedRecord.isPresent()) {
                    list.add(new EmployeeAttendanceRow(emp, savedRecord.get().getStatus(), savedRecord.get().getCheckInTime(), savedRecord.get().getCheckOutTime(), savedRecord.get().getNotes()));
                } else {
                    list.add(new EmployeeAttendanceRow(emp, AttendanceStatus.PRESENT, LocalTime.of(8, 0), LocalTime.of(16, 0), ""));
                }
            }
            Platform.runLater(() -> {
                employeeRows.setAll(list);
                employeeAttendanceTable.setItems(employeeRows);
            });
        }).start();
    }

    // ─── 4. معالجة عمليات الحفظ الجماعي بضغطة زر واحدة ────────────────────
    @FXML
    void handleSaveAllTeachersAttendance() {
        LocalDate date = teacherAttendanceDatePicker.getValue();
        try {
            for (TeacherAttendanceRow row : teacherRows) {
                // تمرير المعاملات كاملة شاملة كائن الأستاذ، التاريخ، الحالة، وقت الدخول، وقت الخروج، والملاحظات
                teacherService.saveAttendance(row.getTeacher(), date, row.getStatus(), row.getCheckInTime(), row.getCheckOutTime(), row.getNotes());
            }
            showAlert(Alert.AlertType.INFORMATION, "تم الحفظ", "تم تحديث وحفظ سجل حضور وأوقات الأساتذة بنجاح.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "خطأ", e.getMessage());
        }
    }

    @FXML
    void handleSaveAllEmployeesAttendance() {
        LocalDate date = employeeAttendanceDatePicker.getValue();
        try {
            for (EmployeeAttendanceRow row : employeeRows) {
                // تمرير المعاملات كاملة شاملة كائن الموظف، التاريخ، الحالة، وقت الدخول، وقت الخروج، والملاحظات
                employeeService.saveAttendance(row.getEmployee(), date, row.getStatus(), row.getCheckInTime(), row.getCheckOutTime(), row.getNotes());
            }
            showAlert(Alert.AlertType.INFORMATION, "تم الحفظ", "تم تحديث وحفظ سجل حضور وأوقات الموظفين بنجاح.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "خطأ", e.getMessage());
        }
    }

    // دالة موحدة لعرض التنبيهات والرسائل
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // ─── كائنات التغليف المؤقتة للأسطر تشمل حقول الوقت الآن (Wrappers) ───
    @Data @AllArgsConstructor
    static class TeacherAttendanceRow {
        private Teacher teacher;
        private AttendanceStatus status;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private String notes;
    }

    @Data @AllArgsConstructor
    static class EmployeeAttendanceRow {
        private Employee employee;
        private AttendanceStatus status;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private String notes;
    }
}