package com.kids.ui;

import com.kids.entities.SchoolYear;
import com.kids.services.SchoolYearService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SettingsController {

    // خدمات Spring
    @Autowired private SchoolYearService yearService;

    // عناصر إعدادات الروضة العامة
    @FXML private TextField txtKindergartenName;
    @FXML private TextField txtKindergartenPhone;
    @FXML private TextField txtKindergartenAddress;

    // عناصر إدارة السنوات الدراسية
    @FXML private TextField txtYearName;
    @FXML private TableView<SchoolYear> yearTable;
    @FXML private TableColumn<SchoolYear, Long> colId;
    @FXML private TableColumn<SchoolYear, String> colYearName;
    @FXML private TableColumn<SchoolYear, Boolean> colStatus;

    private final ObservableList<SchoolYear> yearList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. تهيئة أعمدة جدول السنوات الدراسية
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colYearName.setCellValueFactory(new PropertyValueFactory<>("yearName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("active"));

        // 2. تحميل البيانات
        loadGeneralSettings();
        loadSchoolYearsData();
    }

    // تحميل الإعدادات الافتراضية للروضة
    private void loadGeneralSettings() {
        txtKindergartenName.setText("روضة الإبداع السعيدة");
        txtKindergartenPhone.setText("0555123456");
        txtKindergartenAddress.setText("شارع الحرية، تونس");
    }

    // تحميل قائمة السنوات الدراسية من قاعدة البيانات للجدول
    private void loadSchoolYearsData() {
        yearList.setAll(yearService.getAllYears());
        yearTable.setItems(yearList);
    }

    // معالج حفظ الإعدادات العامة
    @FXML
    private void handleSaveGeneralSettings() {
        // هنا يمكنك حفظ البيانات في جدول الإعدادات لاحقاً
        showNotification("تم الحفظ", "تم تحديث إعدادات الروضة العامة بنجاح.");
    }

    // معالج زر إطلاق وتوليد السنة الدراسية والأشهر تلقائياً
    @FXML
    private void handleCreateYear() {
        String input = txtYearName.getText().trim();

        // التحقق من صحة التنسيق باستخدام الـ Regex (أربعة أرقام - أربعة أرقام)
        if (!input.matches("\\d{4}-\\d{4}")) {
            showErrorNotification("خطأ في الصيغة", "يرجى كتابة الموسم الدراسي بالشكل التالي الحصري: 2026-2027");
            return;
        }

        try {
            // استدعاء الخدمة الذكية لتوليد السنة وأشهرها العشرة دفعة واحدة
            yearService.createSchoolYearWithMonths(input);
            txtYearName.clear();
            loadSchoolYearsData(); // إعادة شحن الجدول بالسطر الجديد
            showNotification("نجاح العملية", "تم إطلاق الموسم الدراسي وتوليد أشهره العشرة (من سبتمبر إلى جوان) بنجاح!");
        } catch (Exception e) {
            showErrorNotification("فشل الحفظ", "حدث خطأ أثناء التوليد، قد تكون السنة مدخلة مسبقاً.");
        }
    }

    // دالات المساعدة لإظهار التنبيهات للمستخدم
    private void showNotification(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorNotification(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}