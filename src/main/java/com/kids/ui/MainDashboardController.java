package com.kids.ui;




import com.kids.configuration.AppContext;
import com.kids.configuration.SpringContextProvider;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
@Component
public class MainDashboardController {

    @Autowired
    private ApplicationContext springContext; // حقن سياق سبرينج في الأعلى

    @FXML private Label lblCurrentYear;
    @FXML private VBox contentArea; // الحاوية المركزية التي سنعرض بداخلها الشاشات الفرعية

    @FXML
    public void initialize() {
        // جلب وعرض السنة الدراسية المثبتة عالمياً عند الدخول
        if (AppContext.getInstance().getCurrentSchoolYear() != null) {
            lblCurrentYear.setText("السنة الدراسية: " + AppContext.getInstance().getCurrentSchoolYear().getYearName());
        }
    }

    /**
     * دالة عامة وذكية لتحميل أي شاشة فرعية FXML داخل المنطقة المركزية للـ Dashboard
     */
    private void setCenterView(String fxmlPath) {
        try {
            // مسح الشاشة الافتراضية أو الشاشة السابقة
            contentArea.getChildren().clear();

            // تحميل واجهة القسم الجديد
            Node node = FXMLLoader.load(getClass().getResource(fxmlPath));

            // جعل الواجهة الفرعية تتمدد لتملأ كامل مساحة الـ Dashboard المركزية
            VBox.setVgrow(node, javafx.scene.layout.Priority.ALWAYS);
            if (node instanceof VBox) {
                ((VBox) node).setMaxWidth(Double.MAX_VALUE);
                ((VBox) node).setMaxHeight(Double.MAX_VALUE);
            }

            contentArea.getChildren().add(node);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("خطأ أثناء تبديل الواجهة الداخلية: " + e.getMessage());
        }
    }

    // --- أحداث أزرار التنقل الجانبية ---

    @FXML
    private void showStudentsView() {
        setCenterView("/fxml/student_management.fxml"); // مسار ملف واجهة الطلاب الخاص بك
    }

    @FXML
    private void showParentsView() {
        setCenterView("/fxml/parents_list.fxml");
    }

    @FXML
    private void showSessionsView() {
        setCenterView("/fxml/sessions_manager.fxml");
    }

    @FXML
    private void showAttendanceView() {
        // الشاشة التي ستحتوي على التصفية والدفتر الذي صممنا الـ SQL له
        setCenterView("/fxml/attendance_register.fxml");
    }

    @FXML
    private void showSettingsView() {
        setCenterView("/fxml/settings.fxml");
    }


    @FXML
    private void handleLogout() {
        try {
            AppContext.getInstance().setCurrentSchoolYear(null);
            Stage stage = (Stage) contentArea.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));

            // الحل السحري: استخدام الـ ContextProvider لمنع الـ NullPointerException نهائياً
            loader.setControllerFactory(SpringContextProvider.getContext()::getBean);

            Parent root = loader.load();

            stage.setScene(new Scene(root, 420, 550));
            stage.setMaximized(false);
            stage.setTitle("تسجيل الدخول");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * الدالة المساعدة لفتح لوحة التحكم لأول مرة والمستدعاة من الـ LoginController
     */
    public static void loadDashboard(Stage currentStage) {
        try {
            Parent root = FXMLLoader.load(MainDashboardController.class.getResource("/fxml/main_dashboard.fxml"));
            Scene scene = new Scene(root);
            currentStage.setTitle("نظام جنة الصغار الإداري");
            currentStage.setScene(scene);
            currentStage.setMaximized(true); // فتح بكامل الشاشة لإعطاء طابع الأنظمة الكبيرة
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
