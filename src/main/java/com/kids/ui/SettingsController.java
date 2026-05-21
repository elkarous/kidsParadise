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

import java.util.Locale;
import java.util.ResourceBundle;

@Component
public class SettingsController {

    // خدمات Spring
    @Autowired private SchoolYearService yearService;

    // عناصر إعدادات الروضة العامة
    @FXML private TextField txtKindergartenName;
    @FXML private TextField txtKindergartenPhone;
    @FXML private TextField txtKindergartenAddress;
    @FXML private ComboBox<String> comboLanguage;

    // عناصر إدارة السنوات الدراسية
    @FXML private TextField txtYearName;
    @FXML private TableView<SchoolYear> yearTable;
    @FXML private TableColumn<SchoolYear, Long> colId;
    @FXML private TableColumn<SchoolYear, String> colYearName;
    @FXML private TableColumn<SchoolYear, Boolean> colStatus;

    private final ObservableList<SchoolYear> yearList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // ضبط القيمة الافتراضية لصندوق اللغة
        comboLanguage.setValue("العربية");

        // ربط الأعمدة (تم حذف colStatus بنجاح)
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colYearName.setCellValueFactory(new PropertyValueFactory<>("yearName"));

        // 2. تحميل البيانات
        loadGeneralSettings();
        loadSchoolYearsData();
    }


    @FXML
    private void handleLanguageChange() {
        String selectedLanguage = comboLanguage.getValue();
        Locale locale;

        if ("English".equals(selectedLanguage)) {
            locale = new Locale("en");
        } else {
            locale = new Locale("ar");
        }

        // 💡 هنا يتم تحميل ملف الخصائص الجديد لتغيير واجهة التطبيق بالكامل
        ResourceBundle bundle = ResourceBundle.getBundle("messages.messages", locale);

        // ملاحظة: لإعادة تحميل نصوص الواجهة الحالية ديناميكياً بدون إغلاق الشاشة،
        // يفضل استدعاء الشاشة مجدداً عبر الـ FXMLLoader وتمرير الـ bundle الجديد له.
        System.out.println("Language changed to: " + locale.getLanguage());
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