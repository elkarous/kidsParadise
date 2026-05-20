package com.kids.ui;


import com.kids.entities.Parent; // تأكد من مطابقة اسم الـ Entity لديك
import com.kids.services.ParentService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ParentFormController {

    @Autowired
    private ParentService parentService;

    @FXML private TextField txtFatherName;
    @FXML private TextField txtMotherName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private Parent parent;
    private boolean saveClicked = false;

    // كونسلوكتور فارغ لـ JavaFX
    public ParentFormController() {}

    @FXML
    public void initialize() {
        // يمكن إضافة قيود على المدخلات هنا إن أردت (مثل منع إدخال حروف في حقل الهاتف)
    }

    /**
     * تمرير بيانات ولي الأمر المراد تعديله، أو كائن جديد تماماً في حالة الإضافة
     */
    public void setParent(Parent parent) {
        this.parent = parent;

        if (parent.getId() != null) {
            // حالة التعديل: ملء الحقول بالبيانات الحالية
            txtFatherName.setText(parent.getFatherName());
            txtMotherName.setText(parent.getMotherName());
            txtPhone.setText(parent.getPhoneNumber()); // تأكد من اسم الحقل في الـ Entity (phoneNumber أو phone)
        } else {
            // حالة إضافة جديدة: تفريغ الحقول
            clearFields();
        }
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            // 1. ربط البيانات المدخلة بالكائن
            parent.setFatherName(txtFatherName.getText().trim());
            parent.setMotherName(txtMotherName.getText().trim());
            parent.setPhoneNumber(txtPhone.getText().trim());
            // 2. الحفظ عبر خدمة سبرينج (سواء كانت العملية Insert أو Update البيئة ستتعامل معها تلقائياً)
            parentService.save(parent);

            // 3. تأكيد الحفظ وإغلاق النافذة
            saveClicked = true;
            closeStage();
        }
    }

    @FXML
    private void handleCancel() {
        saveClicked = false;
        closeStage();
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    private void closeStage() {
        Stage stage = (Stage) btnSave.getScene().getWindow();
        stage.close();
    }

    private void clearFields() {
        txtFatherName.clear();
        txtMotherName.clear();
        txtPhone.clear();
        txtEmail.clear();
    }

    /**
     * التحقق من صحة المدخلات قبل الحفظ لمنع أخطاء قاعدة البيانات
     */
    private boolean isInputValid() {
        String errorMessage = "";

        if (txtFatherName.getText() == null || txtFatherName.getText().isBlank()) {
            errorMessage += "اسم الأب غير صحيح!\n";
        }
        if (txtPhone.getText() == null || txtPhone.getText().isBlank()) {
            errorMessage += "رقم الهاتف مطلوب!\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            // عرض نافذة خطأ تنبيهية للمستخدم
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.initOwner(btnSave.getScene().getWindow());
            alert.setTitle("خطأ في البيانات");
            alert.setHeaderText("يرجى تصحيح الحقول الخاطئة:");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
}
