package com.kids.ui;


import com.kids.entities.Parent;
import com.kids.services.ParentService;
import com.kids.services.PaymentService;
import com.kids.services.TuitionService;
import com.kids.services.UiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.YearMonth;
import java.util.ResourceBundle;

@Component
public class PaymentController {

    @Autowired
    private ParentService parentService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private TuitionService tuitionService;
    @Autowired
    private UiService uiService;
    @Autowired
    private ResourceBundle resourceBundle;

    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> comboMonth;
    @FXML
    private TableView<Parent> paymentTable;
    @FXML
    private TableColumn<Parent, Void> colAction;
    @FXML
    private TableColumn<Parent, String> colParentName;
    @FXML
    private TableColumn<Parent, String> colChildrenCount;
    @FXML
    private TableColumn<Parent, String> colRequiredAmount;
    @FXML
    private TableColumn<Parent, String> colStatus;
    @FXML
    private Label lblTotalExpected;
    @FXML
    private Label lblTotalCollected;
    @FXML
    private Label lblTotalPending;

    private final ObservableList<Parent> parentList = FXCollections.observableArrayList();

    public PaymentController() {
    }

    // داخل دالة initialize المحدثة
    @FXML
    public void initialize() {
        setupMonthComboBox();
        setupTableColumns();
        loadData();

        // تحديث الحسابات المادية عند تغيير الشهر المستهدف
        comboMonth.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                paymentTable.refresh();
                updateFinancialSummary(); // 🌟 تحديث الحسبة السفلية
            }
        });
    }

    /**
     * دالة حساب الإجماليات المالية ديناميكياً
     */
    private void updateFinancialSummary() {
        double totalExpected = 0.0;
        double totalCollected = 0.0;
        String selectedMonth = comboMonth.getValue();

        // المرور على قائمة أولياء الأمور المتواجدة بالجدول حالياً
        for (Parent parent : paymentTable.getItems()) {
            // 1. حساب المستحق على هذا الولي
            double required = tuitionService.calculateMonthlyFeesForParent(parent);
            totalExpected += required;

            // 2. التحقق مما إذا كان قد دفع
            if (paymentService.isMonthPaid(parent.getId(), selectedMonth)) {
                totalCollected += required; // إذا دفع، تنضم القيمة للمداخيل
            }
        }

        // 3. المتبقي بالخارج هو الفارق بين المتوقع والمقبوض
        double totalPending = totalExpected - totalCollected;

        // 4. طباعة القيم المحدثة على الواجهة مع تدوين العملة التونسية
        String currency = " " + resourceBundle.getString("currency.tnd");
        lblTotalExpected.setText(String.format("%.3f", totalExpected) + currency);
        lblTotalCollected.setText(String.format("%.3f", totalCollected) + currency);
        lblTotalPending.setText(String.format("%.3f", totalPending) + currency);
    }

    // تعديل الدالة المدمجة سابقاً لإنعاش الإجماليات بعد نجاح الدفع فوراً
    private void openPaymentPopup(Parent parent, String targetMonth) {
        try {
            Stage dialogStage = new Stage();
            FXMLLoader loader = uiService.openPopup("payment_dialog.fxml", "payment.register.title", paymentTable.getScene().getWindow(), dialogStage);

            PaymentDialogController controller = loader.getController();
            controller.setData(parent, targetMonth);

            dialogStage.showAndWait();

            if (controller.isSuccess()) {
                paymentTable.refresh();
                updateFinancialSummary(); // 🌟 إعادة الحساب فور غلق نافذة تأكيد الدفع بنجاح
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // تعديل دالة loadData لتشغيل الحسبة عند أول دخول للشاشة
    private void loadData() {
        parentList.setAll(parentService.findAll());
        paymentTable.setItems(parentList);
        updateFinancialSummary(); // 🌟 حساب أولي عند فتح التبويب لأول مرة
    }

    private void setupMonthComboBox() {
        // ملء الكومبوبوكس بـالشهر الحالي والأشهر السابقة كمثال لسهولة الاختيار
        YearMonth current = YearMonth.now();
        comboMonth.getItems().addAll(current.toString(), current.minusMonths(1).toString(), current.minusMonths(2).toString());
        comboMonth.getSelectionModel().select(0); // اختيار الشهر الحالي تلقائياً
    }

    private void setupTableColumns() {
        colParentName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFatherName()));

        colChildrenCount.setCellValueFactory(cellData -> new SimpleStringProperty(
                String.valueOf(cellData.getValue().getChildren() != null ? cellData.getValue().getChildren().size() : 0)
        ));

        colRequiredAmount.setCellValueFactory(cellData -> {
            double amount = tuitionService.calculateMonthlyFeesForParent(cellData.getValue());
            return new SimpleStringProperty(amount + " " + resourceBundle.getString("currency.tnd"));
        });

        // عمود الحالة: يتحقق من الـ DB هل الولي دفع لهذا الشهر أم لا
        colStatus.setCellValueFactory(cellData -> {
            String selectedMonth = comboMonth.getValue();
            boolean isPaid = paymentService.isMonthPaid(Long.valueOf(cellData.getValue().getId()), selectedMonth);

            if (isPaid) {
                return new SimpleStringProperty(resourceBundle.getString("payment.status.paid"));
            } else {
                return new SimpleStringProperty(resourceBundle.getString("payment.status.unpaid"));
            }
        });

        // بناء زر إجراء الدفع ديناميكياً داخل الخلايا
        setupActionCells();
    }

    private void setupActionCells() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnPay = new Button();

            {
                btnPay.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
                btnPay.setPrefWidth(100);

                btnPay.setOnAction(event -> {
                    Parent parent = getTableView().getItems().get(getIndex());
                    String selectedMonth = comboMonth.getValue();

                    // منع الدفع المتكرر
                    if (paymentService.isMonthPaid(parent.getId(), selectedMonth)) {
                        showWarningDialog(resourceBundle.getString("payment.alreadyPaid.msg"));
                        return;
                    }

                    openPaymentPopup(parent, selectedMonth);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Parent parent = getTableView().getItems().get(getIndex());
                    String selectedMonth = comboMonth.getValue();

                    // إذا كان مدفوعاً نقوم بتعطيل الزر وتغيير نصه لحماية الخزينة
                    if (paymentService.isMonthPaid(Long.valueOf(parent.getId()), selectedMonth)) {
                        btnPay.setText(resourceBundle.getString("btn.receipt")); // عرض الوصل عوضاً عن الدفع
                        btnPay.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-background-radius: 5;");
                    } else {
                        btnPay.setText(resourceBundle.getString("btn.pay"));
                        btnPay.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 5;-fx-cursor: hand;");
                    }
                    setGraphic(btnPay);
                }
            }
        });
    }


    private void showWarningDialog(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}