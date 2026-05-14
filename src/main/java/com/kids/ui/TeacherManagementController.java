package com.kids.ui;

import com.kids.entities.*;
import com.kids.services.TeacherService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.text.MessageFormat;
import java.time.*;
import java.util.*;

/**
 * JavaFX Controller — Teacher Management Screen

 * Features:
 *  • Teacher list with search
 *  • Daily attendance marking (Present / Late / Absent / Excused)
 *  • Salary summary panel (gross | paid | pending)
 *  • Pay Now button with confirmation dialog
 *  • Bilingual (AR/EN) via ResourceBundle + RTL support
 */
@Component
@RequiredArgsConstructor
public class TeacherManagementController implements Initializable {

    private final TeacherService teacherService;
    private ResourceBundle bundle;

    // ── FXML injections ───────────────────────────────────────────────────────

    @FXML private VBox               rootPane;
    @FXML private Label              lblTitle;
    @FXML private TextField          searchField;
    @FXML private TableView<Teacher> teacherTable;
    @FXML private TableColumn<Teacher, String>  colName;
    @FXML private TableColumn<Teacher, String>  colSpecialty;
    @FXML private TableColumn<Teacher, String>  colSalaryType;
    @FXML private TableColumn<Teacher, String>  colStatus;

    // Attendance panel
    @FXML private DatePicker         attendanceDatePicker;
    @FXML private ToggleGroup        attendanceToggleGroup;
    @FXML private RadioButton        rbPresent;
    @FXML private RadioButton        rbLate;
    @FXML private RadioButton        rbAbsent;
    @FXML private RadioButton        rbExcused;
    @FXML private Spinner<Integer>   sessionsSpinner;
    @FXML private TextArea           attendanceNotes;
    @FXML private Button             btnMarkAttendance;

    // Salary panel
    @FXML private Label              lblGrossSalary;
    @FXML private Label              lblPaid;
    @FXML private Label              lblPending;
    @FXML private ComboBox<TeacherPayment.PaymentMethod> cbPaymentMethod;
    @FXML private TextField          txtReference;
    @FXML private TextField          txtPayAmount;
    @FXML private Button             btnPayNow;

    private final ObservableList<Teacher> teachers = FXCollections.observableArrayList();

    // ── Initialization ────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.bundle = rb;
        applyRtlIfArabic();
        setupTable();
        setupAttendancePanel();
        setupSalaryPanel();
        loadTeachers();
    }

    private void applyRtlIfArabic() {
        if (bundle.getLocale().getLanguage().equals("ar")) {
            rootPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        }
        lblTitle.setText(bundle.getString("nav.teachers"));
    }

    // ── Table Setup ───────────────────────────────────────────────────────────

    private void setupTable() {
        colName.setCellValueFactory(
            data -> new SimpleStringProperty(data.getValue().getName()));

        colSpecialty.setCellValueFactory(
            data -> new SimpleStringProperty(data.getValue().getSpecialty()));

        colSalaryType.setCellValueFactory(data -> {
            Teacher.SalaryType type = data.getValue().getSalaryType();
            String key = type == Teacher.SalaryType.FIXED_MONTHLY
                ? "salary.type.fixed" : "salary.type.per.session";
            return new SimpleStringProperty(bundle.getString(key));
        });

        colStatus.setCellValueFactory(
            data -> new SimpleStringProperty(data.getValue().getStatus().name()));

        teacherTable.setItems(teachers);

        // Update salary panel when a teacher is selected
        teacherTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, selected) -> {
                if (selected != null) refreshSalaryPanel(selected);
            }
        );

        // Search filter
        searchField.textProperty().addListener((obs, old, query) ->
            teacherTable.setItems(
                teachers.filtered(t -> t.getName().toLowerCase()
                    .contains(query.toLowerCase()))
            )
        );
    }

    // ── Attendance Panel ──────────────────────────────────────────────────────

    private void setupAttendancePanel() {
        attendanceDatePicker.setValue(LocalDate.now());

        sessionsSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));

        // Label radio buttons from bundle
        rbPresent.setText(bundle.getString("attendance.present"));
        rbLate.setText(bundle.getString("attendance.late"));
        rbAbsent.setText(bundle.getString("attendance.absent"));
        rbExcused.setText(bundle.getString("attendance.excused"));

        // Hide sessions spinner unless Per Session teacher is selected
        sessionsSpinner.setVisible(false);
        teacherTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, t) -> {
                if (t != null) {
                    sessionsSpinner.setVisible(
                        t.getSalaryType() == Teacher.SalaryType.PER_SESSION);
                }
            }
        );

        btnMarkAttendance.setText(bundle.getString("attendance.mark"));
        btnMarkAttendance.setOnAction(e -> handleMarkAttendance());
    }

    @FXML
    private void handleMarkAttendance() {
        Teacher selected = teacherTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, bundle.getString("msg.no.data"));
            return;
        }

        TeacherAttendance.AttendanceStatus status = getSelectedAttendanceStatus();
        LocalDate date     = attendanceDatePicker.getValue();
        int       sessions = sessionsSpinner.getValue();
        String    notes    = attendanceNotes.getText();

        try {
            teacherService.markAttendance(selected.getId(), date, status, sessions, notes);
            showAlert(Alert.AlertType.INFORMATION, bundle.getString("msg.saved"));
            refreshSalaryPanel(selected);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, ex.getMessage());
        }
    }

    private TeacherAttendance.AttendanceStatus getSelectedAttendanceStatus() {
        Toggle selected = attendanceToggleGroup.getSelectedToggle();
        if (selected == rbPresent) return TeacherAttendance.AttendanceStatus.PRESENT;
        if (selected == rbLate)    return TeacherAttendance.AttendanceStatus.LATE;
        if (selected == rbAbsent)  return TeacherAttendance.AttendanceStatus.ABSENT;
        return TeacherAttendance.AttendanceStatus.EXCUSED;
    }

    // ── Salary Panel ──────────────────────────────────────────────────────────

    private void setupSalaryPanel() {
        cbPaymentMethod.setItems(
            FXCollections.observableArrayList(TeacherPayment.PaymentMethod.values()));
        cbPaymentMethod.getSelectionModel().selectFirst();

        btnPayNow.setText(bundle.getString("payroll.pay.now"));
        btnPayNow.setOnAction(e -> handlePayNow());
    }

    private void refreshSalaryPanel(Teacher teacher) {
        YearMonth month = YearMonth.now(); // replace with monthPicker.getValue()

        // Run on background thread to avoid blocking JavaFX thread
        new Thread(() -> {
            BigDecimal gross   = teacherService.calculateGrossSalary(teacher.getId(), month);
            BigDecimal pending = teacherService.calculatePendingSalary(teacher.getId(), month);
            BigDecimal paid    = gross.subtract(pending);

            Platform.runLater(() -> {
                lblGrossSalary.setText(bundle.getString("payroll.gross") + ": " + gross + " DT");
                lblPaid.setText(bundle.getString("payroll.paid") + ": " + paid + " DT");
                lblPending.setText(bundle.getString("salary.pending") + ": " + pending + " DT");

                // Pre-fill pay amount with full pending
                txtPayAmount.setText(pending.toPlainString());
                btnPayNow.setDisable(pending.compareTo(BigDecimal.ZERO) == 0);
            });
        }).start();
    }

    @FXML
    private void handlePayNow() {
        Teacher selected = teacherTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        BigDecimal amount;
        try {
            amount = new BigDecimal(txtPayAmount.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid amount / مبلغ غير صحيح");
            return;
        }

        // Confirmation dialog
        String confirmMsg = MessageFormat.format(
            bundle.getString("msg.confirm.pay"),
            amount.toPlainString(),
            selected.getName()
        );

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, confirmMsg,
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle(bundle.getString("payroll.pay.now"));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    teacherService.recordPayment(
                        selected.getId(),
                        YearMonth.now(),
                        amount,
                        cbPaymentMethod.getValue(),
                        txtReference.getText()
                    );
                    showAlert(Alert.AlertType.INFORMATION, bundle.getString("msg.payment.success"));
                    refreshSalaryPanel(selected);
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, ex.getMessage());
                }
            }
        });
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    private void loadTeachers() {
        // In a real app, inject TeacherRepository or use a TeacherService.findAll()
        // teachers.setAll(teacherService.findAllActive());
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private void showAlert(Alert.AlertType type, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type, message, ButtonType.OK);
            alert.showAndWait();
        });
    }
}
