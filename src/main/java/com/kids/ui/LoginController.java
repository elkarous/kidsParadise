package com.kids.ui;

import com.kids.configuration.AppContext;
import com.kids.entities.SchoolYear;
import com.kids.services.SchoolYearService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.List;

@Component
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<SchoolYear> comboSchoolYear; // قائمة السنوات الدراسية تحت حقول الإدخال
    @FXML private Button loginButton;

    @Autowired
    private SchoolYearService schoolYearService;

    @FXML
    public void initialize() {
        // 1. جلب كل السنوات وتعبئة الـ ComboBox
        List<SchoolYear> allYears = schoolYearService.getAllYears();
// طباعة الحجم للتأكد
        System.out.println("Data size from DB: " + allYears.size());

// طباعة قيمة حقل محدد يدوياً للتأكد من نجاح الـ Mapping
        if (!allYears.isEmpty() && allYears.get(0) != null) {
            System.out.println("First Year Name: " + allYears.get(0).getYearName());
        } else {
            System.out.println("The list contains actual null pointers!");
        }
        comboSchoolYear.setItems(FXCollections.observableArrayList(allYears));

        // 2. ضبط الـ Converter لكي يظهر اسم السنة فقط (مثال: "2025-2026")
        comboSchoolYear.setConverter(new StringConverter<>() {
            @Override
            public String toString(SchoolYear year) {
                return year == null ? "" : year.getYearName();
            }
            @Override
            public SchoolYear fromString(String string) {
                return null;
            }
        });

        // 3. السحر هنا: تحديد السنة الحالية تلقائياً كخيار افتراضي
        SchoolYear actualYear = schoolYearService.findActualYear();
        if (actualYear != null) {
            comboSchoolYear.getSelectionModel().select(actualYear);
        } else if (!allYears.isEmpty()) {
            // كخطة بديلة (Fallback): إذا لم يطابق تاريخ اليوم أي سنة، اختر أول سنة في القائمة
            comboSchoolYear.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (isValidLogin(username, password)) {
            SchoolYear selectedYear = comboSchoolYear.getSelectionModel().getSelectedItem();
            if (selectedYear == null) {
                showError("الرجاء اختيار سنة دراسية ممتدة قبل الدخول.");
                return;
            }

            // 1. تثبيت السنة الدراسية عالمياً في الـ AppContext
            AppContext.getInstance().setCurrentSchoolYear(selectedYear);

            // 2. جلب الـ Stage الحالي الخاص بشاشة تسجيل الدخول
            Stage currentStage = (Stage) loginButton.getScene().getWindow();

            // 3. القفز فوراً إلى لوحة التحكم الرئيسية بأمان
            MainDashboardController.loadDashboard(currentStage);

        } else {
            showError("اسم المستخدم أو كلمة المرور غير صحيحة.");
        }
    }

    private boolean isValidLogin(String user, String pass) {
        // منطق التحقق الخاص بك هنا
        return "admin".equals(user) && "admin".equals(pass);
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }}