package com.kids.ui;

import com.kids.entities.Parent;
import com.kids.entities.WorkingMonth;
import com.kids.services.PaymentService;
import com.kids.services.TuitionService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

@Component
public class PaymentDialogController {

    @Autowired private PaymentService paymentService;
    @Autowired private TuitionService tuitionService;
    @Autowired private ResourceBundle resourceBundle;

    @FXML private Label lblParentName;
    @FXML private Label lblTargetMonth;
    @FXML private Label lblChildrenCount;
    @FXML private Label lblBaseAmount;
    @FXML private Label lblDiscount;
    @FXML private Label lblFinalAmount;
    @FXML private ComboBox<String> comboMethod;
    @FXML private TextField txtNotes;
    @FXML private Button btnConfirm;

    private Parent parent;
    private WorkingMonth workingMonth; // 🌟 تم التغيير من String إلى كائن WorkingMonth
    private boolean success = false;

    public PaymentDialogController() {}

    @FXML
    public void initialize() {
        // تعبئة طرق الدفع من ملف اللغات إن أمكن، أو تركها كالتالي مع التناسق المودرن
        comboMethod.getItems().addAll("Cash", "Cheque", "Bank Transfer");
        comboMethod.getSelectionModel().select(0);
    }

    public void setData(Parent parent, WorkingMonth targetMonth) {
        this.parent = parent;
        this.workingMonth = targetMonth;

        int childrenCount = parent.getChildren() != null ? parent.getChildren().size() : 0;
        double baseAmount = childrenCount * 100.0; // 100 د عن كل طفل كمثال
        double finalAmount = tuitionService.calculateMonthlyFeesForParent(parent);
        double discount = baseAmount - finalAmount;

        // عرض الحسبة المالية الآمنة أمام الموظف
        lblParentName.setText(parent.getFatherName());
        lblTargetMonth.setText(workingMonth.getMonthName());
        lblChildrenCount.setText(String.valueOf(childrenCount));
        lblBaseAmount.setText(baseAmount + " " + resourceBundle.getString("currency.tnd"));
        lblDiscount.setText(discount + " " + resourceBundle.getString("currency.tnd"));
        lblFinalAmount.setText(finalAmount + " " + resourceBundle.getString("currency.tnd"));
    }

    @FXML
    private void handleConfirm() {
        try {
            // 🌟 استدعاء دالة الخدمة المحدثة التي تقبل كائن الـ workingMonth لحفظ الفاتورة برمجياً بصيغة نظيفة
            paymentService.registerMonthlyPayment(
                    parent,
                    workingMonth,
                    comboMethod.getValue(),
                    txtNotes.getText().trim()
            );

            success = true;
            closeStage();
        } catch (Exception e) {
            // استخدام Alert منسق لعرض رسالة الخطأ للموظف (مثال: "الشهر مغلق" أو "تم الدفع مسبقاً")
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("خطأ في العملية المالية");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleCancel() {
        success = false;
        closeStage();
    }

    public boolean isSuccess() {
        return success;
    }

    private void closeStage() {
        if (btnConfirm.getScene() != null && btnConfirm.getScene().getWindow() != null) {
            Stage stage = (Stage) btnConfirm.getScene().getWindow();
            stage.close();
        }
    }
}