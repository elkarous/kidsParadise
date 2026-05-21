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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MainDashboardController {

    // جعل الحقن يمر عبر @Autowired على مستوى الحقول أو الكونسلوكتور بشكل متناسق
    @Autowired
    private ConfigurableApplicationContext springContext;

    @Autowired
    private java.util.ResourceBundle resourceBundle;

    @FXML private Label lblCurrentYear;
    @FXML private VBox contentArea;

    // كونسلوكتور فارغ إلزامي لـ JavaFX وسبرينج سيتولى حقن الحقول بالأعلى تلقائياً
    public MainDashboardController() {
    }

    @FXML
    public void initialize() {
        // جلب وعرض السنة الدراسية المثبتة عالمياً عند الدخول
        if (AppContext.getInstance().getCurrentSchoolYear() != null) {
            lblCurrentYear.setText("السنة الدراسية: " + AppContext.getInstance().getCurrentSchoolYear().getYearName());
        }
    }

    private void setCenterView(String fxmlPath) {
        try {
            contentArea.getChildren().clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            // 1. تزويد الـ Loader بملف الترجمة لحل خطأ "No resources specified"
            loader.setResources(resourceBundle);

            // 2. إجبار JavaFX على جلب الـ Controller من قلب Spring Boot بأمان
            loader.setControllerFactory(springContext::getBean);

            Node node = loader.load();

            VBox.setVgrow(node, javafx.scene.layout.Priority.ALWAYS);
            if (node instanceof VBox) {
                ((VBox) node).setMaxWidth(Double.MAX_VALUE);
                ((VBox) node).setMaxHeight(Double.MAX_VALUE);
            }

            contentArea.getChildren().add(node);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("خطأ أثناء تحميل الواجهة الداخلية: " + e.getMessage());
        }
    }

    // --- أحداث أزرار التنقل الجانبية ---

    @FXML
    private void showStudentsView() {
        setCenterView("/fxml/student_management.fxml");
    }

    @FXML
    private void showParentsView() {
        setCenterView("/fxml/parents_management.fxml");
    }

    @FXML
    private void showSessionsView() {
        setCenterView("/fxml/spaces_management.fxml");
    }

    @FXML
    private void showAttendanceView() {
        setCenterView("/fxml/attendance_register.fxml");
    }
    @FXML
    private void showTeachersView() {
        setCenterView("/fxml/teacher_management.fxml");
    }

    @FXML
    private void showEmployeView() {
        setCenterView("/fxml/admin_manager.fxml");
    }
    @FXML
    private void showPaymentsView() {
        setCenterView("/fxml/payments_manager.fxml"); // المسار الفعلي لملف واجهة المدفوعات الرئيسي
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

            // استخدام الـ ContextProvider لمنع الـ NullPointerException عند الخروج
            loader.setControllerFactory(SpringContextProvider.getContext()::getBean);
            loader.setResources(resourceBundle);

            Parent root = loader.load();

            stage.setScene(new Scene(root, 420, 550));
            stage.setMaximized(false);
            stage.setTitle("تسجيل الدخول");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("خطأ أثناء تسجيل الخروج: " + e.getMessage());
        }
    }

    /**
     * الدالة المساعدة المحدثة والآمنة لفتح لوحة التحكم لأول مرة من الـ LoginController
     */
    public static void loadDashboard(Stage currentStage) {
        try {
            FXMLLoader loader = new FXMLLoader(MainDashboardController.class.getResource("/fxml/main_dashboard.fxml"));

            // التعديل السحري: ربط شاشة الـ Dashboard بسبرينج منذ لحظة ولادتها الأولى!
            loader.setControllerFactory(SpringContextProvider.getContext()::getBean);
            loader.setResources(SpringContextProvider.getContext().getBean(java.util.ResourceBundle.class));

            Parent root = loader.load();
            Scene scene = new Scene(root);
            currentStage.setTitle("نظام جنة الصغار الإداري");
            currentStage.setScene(scene);
            currentStage.setMaximized(true);
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("خطأ أثناء تحميل لوحة التحكم الرئيسية: " + e.getMessage());
        }
    }
}