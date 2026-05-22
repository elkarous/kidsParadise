package com.kids.ui;

import com.kids.entities.Employee;
import com.kids.entities.SalaryType;
import com.kids.entities.Status;
import com.kids.entities.Teacher;
import com.kids.services.EmployeeService;
import com.kids.services.TeacherService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class StuffFormController {
    @Autowired
    private final TeacherService teacherService;

    @Autowired
    private final EmployeeService employeeService;

    @FXML private Label lblFormTitle;
    @FXML private TextField txtName;
    @FXML private TextField txtSpecialty;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private DatePicker dpHiringDate;
    @FXML private ComboBox<Status> cbStatus;
    @FXML private ComboBox<SalaryType> cbSalaryType;
    @FXML private TextField txtBaseSalary;
    @FXML private VBox paneAbsencePenalty;
    @FXML private TextField txtAbsencePenalty;

    @FXML private Button btnSave;
    @FXML private Button btnDelete;

    private Teacher currentStaff;
    private Employee employee;
    private boolean isEditMode = false;
    private Runnable onSaveCallback;

    public void setStaffData(Teacher staff, boolean isTeacherTab, Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;

        // ربط الـ Enums الحقيقية الموجودة في الـ Database entity
        cbSalaryType.setItems(FXCollections.observableArrayList(SalaryType.values()));
        cbStatus.setItems(FXCollections.observableArrayList(Status.values()));

        // التحكم في إخفاء وإظهار حقل خصم الغياب بناءً على الـ SalaryType المختار فعلياً
        cbSalaryType.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isFixed = (newVal == SalaryType.FIXED_MONTHLY);
            paneAbsencePenalty.setVisible(isFixed);
            paneAbsencePenalty.setManaged(isFixed);
        });

        if (staff != null) {
            this.currentStaff = staff;
            this.isEditMode = true;

            lblFormTitle.setText("تعديل البيانات الأساسية");
            txtName.setText(staff.getName());
            txtSpecialty.setText(staff.getSpecialty());
            txtPhone.setText(staff.getPhone());
            txtEmail.setText(staff.getEmail());
            dpHiringDate.setValue(staff.getHiringDate());
            cbStatus.setValue(staff.getStatus());
            cbSalaryType.setValue(staff.getSalaryType());
            txtBaseSalary.setText(staff.getBaseSalary() != null ? staff.getBaseSalary().toPlainString() : "0");
            txtAbsencePenalty.setText(staff.getAbsencePenaltyPerDay() != null ? staff.getAbsencePenaltyPerDay().toPlainString() : "0");

            btnSave.setText("تحديث");
            btnDelete.setVisible(true);
        } else {
            this.currentStaff = new Teacher();
            this.isEditMode = false;

            lblFormTitle.setText("إضافة سجل جديد");
            txtName.clear();
            txtSpecialty.clear();
            txtPhone.clear();
            txtEmail.clear();
            dpHiringDate.setValue(LocalDate.now());
            cbStatus.setValue(Status.ACTIVE);
            cbSalaryType.setValue(SalaryType.FIXED_MONTHLY);
            txtBaseSalary.setText("0");
            txtAbsencePenalty.setText("0");

            btnSave.setText("حفظ");
            btnDelete.setVisible(false);
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (txtName.getText().trim().isEmpty() || dpHiringDate.getValue() == null) {
            new Alert(Alert.AlertType.WARNING, "يرجى ملء الحقول الإجبارية (الاسم وتاريخ التعيين)", ButtonType.OK).showAndWait();
            return;
        }

        try {
            // ملء حقول الـ Entity المباشرة
            if(this.currentStaff != null) {
                currentStaff.setName(txtName.getText().trim());
                currentStaff.setSpecialty(txtSpecialty.getText().trim());
                currentStaff.setPhone(txtPhone.getText().trim());
                currentStaff.setEmail(txtEmail.getText().trim());
                currentStaff.setHiringDate(dpHiringDate.getValue());
                currentStaff.setStatus(cbStatus.getValue());
                currentStaff.setSalaryType(cbSalaryType.getValue());

                // تحويل المدخلات إلى BigDecimal متوافق مع الحقول المالية في الـ DB
                currentStaff.setBaseSalary(new BigDecimal(txtBaseSalary.getText().trim()));

                if (cbSalaryType.getValue() == SalaryType.FIXED_MONTHLY) {
                    currentStaff.setAbsencePenaltyPerDay(new BigDecimal(txtAbsencePenalty.getText().trim()));
                } else {
                    currentStaff.setAbsencePenaltyPerDay(BigDecimal.ZERO);
                }

                // الحفظ عبر الـ Service
                teacherService.save(currentStaff);
            }else {
                employee.setName(txtName.getText().trim());
                employee.setRole(txtSpecialty.getText().trim());
                employee.setPhone(txtPhone.getText().trim());
                employee.setEmail(txtEmail.getText().trim());
                employee.setHiringDate(dpHiringDate.getValue());


                // تحويل المدخلات إلى BigDecimal متوافق مع الحقول المالية في الـ DB
                employee.setBaseSalary(new BigDecimal(txtBaseSalary.getText().trim()));


                // الحفظ عبر الـ Service
                employeeService.save(employee);
            }

            if (onSaveCallback != null) onSaveCallback.run();
            closeStage();

        } catch (NumberFormatException nfe) {
            new Alert(Alert.AlertType.ERROR, "خطأ في صيغة الأرقام المالية المدخلة.", ButtonType.OK).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "خطأ أثناء الحفظ: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (currentStaff == null || currentStaff.getId() == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "هل تريد حذف هذا السجل نهائياً؟", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    teacherService.deleteById(currentStaff.getId());
                    if (onSaveCallback != null) onSaveCallback.run();
                    closeStage();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "فشل الحذف لارتباط السجل ببيانات أخرى: " + ex.getMessage(), ButtonType.OK).showAndWait();
                }
            }
        });
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.close();
    }

    public void setEmployeeData(Employee staff, boolean isTeacherTab, Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;

        // ربط الـ Enums الحقيقية الموجودة في الـ Database entity
        cbSalaryType.setItems(FXCollections.observableArrayList(SalaryType.values()));
        cbStatus.setItems(FXCollections.observableArrayList(Status.values()));

        // التحكم في إخفاء وإظهار حقل خصم الغياب بناءً على الـ SalaryType المختار فعلياً
        cbSalaryType.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isFixed = (newVal == SalaryType.FIXED_MONTHLY);
            paneAbsencePenalty.setVisible(isFixed);
            paneAbsencePenalty.setManaged(isFixed);
        });

        if (staff != null) {
            this.employee = staff;
            this.isEditMode = true;

            lblFormTitle.setText("تعديل البيانات الأساسية");
            txtName.setText(staff.getName());
            txtSpecialty.setText(staff.getRole());
            txtPhone.setText(staff.getPhone());
            txtEmail.setText(staff.getEmail());
            dpHiringDate.setValue(staff.getHiringDate());
            txtBaseSalary.setText(staff.getBaseSalary() != null ? staff.getBaseSalary().toPlainString() : "0");
            btnSave.setText("تحديث");
            btnDelete.setVisible(true);
        } else {
            this.employee = new Employee();
            this.isEditMode = false;

            lblFormTitle.setText("إضافة سجل جديد");
            txtName.clear();
            txtSpecialty.clear();
            txtPhone.clear();
            txtEmail.clear();
            dpHiringDate.setValue(LocalDate.now());
            cbStatus.setValue(Status.ACTIVE);
            cbSalaryType.setValue(SalaryType.FIXED_MONTHLY);
            txtBaseSalary.setText("0");
            txtAbsencePenalty.setText("0");

            btnSave.setText("حفظ");
            btnDelete.setVisible(false);
        }
    }

}