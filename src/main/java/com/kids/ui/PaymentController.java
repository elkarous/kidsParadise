package com.kids.ui;

import com.kids.entities.Parent;
import com.kids.entities.WorkingMonth;
import com.kids.repositories.WorkingMonthRepository;
import com.kids.services.ParentService;
import com.kids.services.PaymentService;
import com.kids.services.TuitionService;
import com.kids.services.UiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class PaymentController {

    @Autowired private ParentService parentService;
    @Autowired private PaymentService paymentService;
    @Autowired private TuitionService tuitionService;
    @Autowired private WorkingMonthRepository workingMonthRepository; // جلب الأشهر
    @Autowired private UiService uiService;
    @Autowired private ResourceBundle resourceBundle;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<WorkingMonth> comboMonth; // تم تعديله ليتعامل مع كائن WorkingMonth
    @FXML private TableView<Parent> paymentTable; // يعرض قائمة الأولياء تماماً مثل الكود القديم
    @FXML private TableColumn<Parent, Void> colAction;
    @FXML private TableColumn<Parent, String> colParentName;
    @FXML private TableColumn<Parent, String> colChildrenCount;
    @FXML private TableColumn<Parent, String> colRequiredAmount;
    @FXML private TableColumn<Parent, String> colStatus;

    @FXML private Label lblTotalExpected;
    @FXML private Label lblTotalCollected;
    @FXML private Label lblTotalPending;

    private final ObservableList<Parent> parentList = FXCollections.observableArrayList();
    private FilteredList<Parent> filteredParentList; // لإضافة خاصية البحث بالاسم حركياً

    public PaymentController() {}

    @FXML
    public void initialize() {
        setupMonthComboBox();
        setupTableColumns();
        loadData();
        setupFilteringLogic();

        // تحديث الجدول والحسابات المادية فوراً عند تغيير الشهر الدراسي من الكومبوبوكس
        comboMonth.valueProperty().addListener((obs, oldVal, newVal) -> {
            paymentTable.refresh();
            updateFinancialSummary();
        });
        updateFinancialSummary();
    }

    /**
     * تهيئة الكومبوبوكس ليعرض أسماء كائنات الأشهر الدراسية القادمة من قاعدة البيانات
     */
    private void setupMonthComboBox() {
        comboMonth.setConverter(new StringConverter<WorkingMonth>() {
            @Override
            public String toString(WorkingMonth month) {
                if (month == null) return "";
                return month.getMonthName();
            }

            @Override
            public WorkingMonth fromString(String string) {
                return null;
            }
        });
    }

    /**
     * تحميل أولياء الأمور وتحديد الشهر النشط كقيمة افتراضية عند الإقلاع
     */
    private void loadData() {
        // 1. تحميل الأولياء
        parentList.setAll(parentService.findAll());

        // 2. تحميل الأشهر وتحديد النشط منها تلقائياً
        List<WorkingMonth> allMonths = workingMonthRepository.findAll();
        comboMonth.setItems(FXCollections.observableArrayList(allMonths));

        WorkingMonth defaultMonth = allMonths.stream()
                .filter(month -> !month.isClosed())
                .findFirst()
                .orElse(!allMonths.isEmpty() ? allMonths.get(0) : null);

        if (defaultMonth != null) {
            comboMonth.setValue(defaultMonth);
        }

        updateFinancialSummary();
    }

    /**
     * إعداد البحث النصي المودرن باسم ولي الأمر ليعمل مع الجدول
     */
    private void setupFilteringLogic() {
        FilteredList<Parent> filteredPaymentList = new FilteredList<>(parentList, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredPaymentList.setPredicate(parent -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String filter = newValue.toLowerCase().trim();
                return parent.getFatherName().toLowerCase().contains(filter);
            });
            updateFinancialSummary(); // إعادة تحديث المجموع بناءً على نتائج البحث
        });

        paymentTable.setItems(filteredPaymentList);
    }

    /**
     * حساب الإجماليات المالية ديناميكياً بناءً على الشهر الدراسي الكائن المختار
     */
    private void updateFinancialSummary() {
        double totalExpected = 0.0;
        double totalCollected = 0.0;
        WorkingMonth selectedMonth = comboMonth.getValue();

        if (selectedMonth == null) return;

        // حساب المبالغ بناءً على الأسطر الظاهرة بالجدول حالياً (المفلترة)
        for (Parent parent : paymentTable.getItems()) {
            double required = tuitionService.calculateMonthlyFeesForParent(parent);
            totalExpected += required;

            // استخدام الـ ID الخاص بكائن الشهر للدقة والأمان
            if (paymentService.isMonthPaid(parent.getId(), selectedMonth.getId())) {
                totalCollected += required;
            }
        }

        double totalPending = totalExpected - totalCollected;

        String currency = " " + resourceBundle.getString("currency.tnd");
        lblTotalExpected.setText(String.format("%.3f", totalExpected) + currency);
        lblTotalCollected.setText(String.format("%.3f", totalCollected) + currency);
        lblTotalPending.setText(String.format("%.3f", totalPending) + currency);
    }

    private void setupTableColumns() {
        colParentName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFatherName()));

        colChildrenCount.setCellValueFactory(cellData -> new SimpleStringProperty(
                String.valueOf(cellData.getValue().getChildren() != null ? cellData.getValue().getChildren().size() : 0)
        ));

        colRequiredAmount.setCellValueFactory(cellData -> {
            double amount = tuitionService.calculateMonthlyFeesForParent(cellData.getValue());
            return new SimpleStringProperty(String.format("%.3f", amount) + " " + resourceBundle.getString("currency.tnd"));
        });

        // عمود الحالة المطور المعتمد على كائن الشهر الدراسي
        colStatus.setCellValueFactory(cellData -> {
            WorkingMonth selectedMonth = comboMonth.getValue();
            if (selectedMonth == null) return new SimpleStringProperty("");

            boolean isPaid = paymentService.isMonthPaid(cellData.getValue().getId(), selectedMonth.getId());
            return new SimpleStringProperty(isPaid ?
                    resourceBundle.getString("payment.status.paid") :
                    resourceBundle.getString("payment.status.unpaid"));
        });

        setupActionCells();
        updateFinancialSummary();
    }

    /**
     * 🌟 بناء وتحديث زر الدفع ديناميكياً حسب حالة الدفع للولي في الشهر المختار
     */
    private void setupActionCells() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnPay = new Button();

            {
                btnPay.setPrefWidth(110);
                btnPay.getStyleClass().add("btn-primary"); // الاعتماد على الـ CSS الأساسي

                btnPay.setOnAction(event -> {
                    Parent parent = getTableView().getItems().get(getIndex());
                    WorkingMonth selectedMonth = comboMonth.getValue();

                    if (selectedMonth == null) return;

                    // في حال دفع مسبقاً، يعرض الوصل أو يمنع التكرار
                    if (paymentService.isMonthPaid(parent.getId(), selectedMonth.getId())) {
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
                    WorkingMonth selectedMonth = comboMonth.getValue();

                    if (selectedMonth != null && paymentService.isMonthPaid(parent.getId(), selectedMonth.getId())) {
                        // 1. حالة الولي دافع: يتحول الزر للون أزرق/معاينة الوصل
                        btnPay.setText(resourceBundle.getString("btn.receipt"));
                        btnPay.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
                    } else {
                        // 2. حالة الولي لم يدفع: يظهر زر الدفع الأخضر المعتاد
                        btnPay.setText(resourceBundle.getString("btn.pay"));
                        btnPay.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
                    }
                    setGraphic(btnPay);
                }
            }
        });
    }

    private void openPaymentPopup(Parent parent, WorkingMonth targetMonth) {
        try {
            Stage dialogStage = new Stage();
            FXMLLoader loader = uiService.openPopup("payment_dialog.fxml", "payment.register.title", paymentTable.getScene().getWindow(), dialogStage);

            PaymentDialogController controller = loader.getController();
            controller.setData(parent, targetMonth); // تمرير الكائن الكامل للـ Dialog

            dialogStage.showAndWait();

            if (controller.isSuccess()) {
                paymentTable.refresh();
                updateFinancialSummary(); // إنعاش فوري للحسابات والجدول عند الحفظ
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showWarningDialog(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}