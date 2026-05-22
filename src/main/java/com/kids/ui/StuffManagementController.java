package com.kids.ui;

import com.kids.entities.Employee;
import com.kids.entities.SalaryType;
import com.kids.entities.Teacher;
import com.kids.services.EmployeeService;
import com.kids.services.TeacherService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class StuffManagementController implements Initializable {

    @Autowired
    private final TeacherService teacherService;
    @Autowired
    private final EmployeeService employeeService;
    @Autowired
    private final ApplicationContext springContext; // مطلوب لتحميل الـ Form المتوافق مع Spring Boot
    @Autowired
    private final ResourceBundle bundle;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> comboTeacherSalaryType;
    @FXML
    private TableView<Teacher> teacherTable;
    @FXML
    private TableColumn<Teacher, String> colName;
    @FXML
    private TableColumn<Teacher, String> colSpecialty;
    @FXML
    private TableColumn<Teacher, String> colSalaryType;
    @FXML
    private TableColumn<Teacher, String> colStatus;

    @FXML
    private TextField searchEmployeeField;
    @FXML
    private ComboBox<String> comboEmployeeSalaryType;
    @FXML
    private TableView<Employee> employeeTable;
    @FXML
    private TableColumn<Teacher, Void> colTeacherAction;
    @FXML
    private TableColumn<Employee, Void> colEmployeeAction;

    @FXML
    private TableColumn<Employee, String> colEmployeeName;
    @FXML
    private TableColumn<Employee, String> colEmployeeRole;
    @FXML
    private TableColumn<Employee, String> colEmployeeSalaryType;
    @FXML
    private TableColumn<Employee, String> colEmployeeStatus;

    private final ObservableList<Teacher> masterTeachers = FXCollections.observableArrayList();
    private final ObservableList<Employee> masterEmployees = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupDropdowns();
        setupTableColumns();
        setupTableClickListeners(); // 🆕 تفعيل دبل كليك الماوس
        loadData();
    }

    private void setupTableColumns() {
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colSpecialty.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSpecialty()));
        colSalaryType.setCellValueFactory(data -> new SimpleStringProperty(bundle.getString(data.getValue().getSalaryType() == SalaryType.FIXED_MONTHLY ? "salary.type.fixed" : "salary.type.per.session")));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));

        colEmployeeName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colEmployeeRole.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        colEmployeeStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        setupActionHeaderAndCells();
    }

    // 🆕 ميثود رصد النقرات المزدوجة للماوس على الجداول لفتح نافذة التعديل
    private void setupTableClickListeners() {
        teacherTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                Teacher selected = teacherTable.getSelectionModel().getSelectedItem();
                if (selected != null) openFormWindow(selected, true);
            }
        });

        employeeTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                Employee selected = employeeTable.getSelectionModel().getSelectedItem();
                if (selected != null) openFormWindowEmployee(selected, false);
            }
        });
    }

    @FXML
    public void handleAddNewTeacher(ActionEvent event) {
        openFormWindow(null, true);
    }

    @FXML
    public void handleAddNewEmployee(ActionEvent event) {
        openFormWindowEmployee(null, false);
    }

    // دالة فتح النافذة المنفصلة وتغذيتها بالبيانات حركياً
    private void openFormWindow(Teacher staff, boolean isTeacherTab) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/stuff-form.fxml"), bundle);
            loader.setControllerFactory(springContext::getBean); // تضمن ربط الـ Core الخاص بـ Spring Boot
            Parent root = loader.load();

            StuffFormController formController = loader.getController();
            // نمرر البيانات والدالة السرجية لتحديث البيانات فور الحفظ تلقائياً
            formController.setStaffData(staff, isTeacherTab, this::loadData);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // تجعل الشاشة منبثقة وقافلة للخلفية لقفل التداخل
            stage.setTitle(staff == null ? "إضافة إدخال جديد" : "تعديل البيانات");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupDropdowns() {
        ObservableList<String> salaryTypes = FXCollections.observableArrayList(bundle.getString("salary.type.fixed"), bundle.getString("salary.type.per.session"), "الكل / Tous");
        comboTeacherSalaryType.setItems(salaryTypes);
        comboTeacherSalaryType.getSelectionModel().selectLast();
        comboEmployeeSalaryType.setItems(salaryTypes);
        comboEmployeeSalaryType.getSelectionModel().selectLast();

        searchField.textProperty().addListener((obs, old, val) -> applyTeacherFilter());
        comboTeacherSalaryType.valueProperty().addListener((obs, old, val) -> applyTeacherFilter());
        searchEmployeeField.textProperty().addListener((obs, old, val) -> applyEmployeeFilter());
        comboEmployeeSalaryType.valueProperty().addListener((obs, old, val) -> applyEmployeeFilter());
    }

    private void applyTeacherFilter() {
        String query = searchField.getText().toLowerCase().trim();
        String selectedSalary = comboTeacherSalaryType.getValue();
        teacherTable.setItems(masterTeachers.filtered(t -> {
            boolean matchesSearch = t.getName().toLowerCase().contains(query) || t.getSpecialty().toLowerCase().contains(query);
            String salaryKey = t.getSalaryType() == SalaryType.FIXED_MONTHLY ? bundle.getString("salary.type.fixed") : bundle.getString("salary.type.per.session");
            return matchesSearch && (selectedSalary.equals("الكل / Tous") || salaryKey.equals(selectedSalary));
        }));
    }

    private void applyEmployeeFilter() {
        String query = searchEmployeeField.getText().toLowerCase().trim();
        employeeTable.setItems(masterEmployees.filtered(e -> e != null &&  e.getName().toLowerCase().contains(query)));
    }

    private void loadData() {
        var teachers = teacherService.findAllActive();
        var employees = employeeService.findAll();
        ObservableList<Teacher> teachersList = FXCollections.observableArrayList(teachers);
        ObservableList<Employee> employeesList = FXCollections.observableArrayList(employees);

        masterTeachers.setAll(teachersList);
        masterEmployees.setAll(employeesList);
        applyTeacherFilter();
        applyEmployeeFilter();
    }

    private void setupActionHeaderAndCells() {
        // تحسين الـ Graphics بالـ CSS المتناسق مع الهوية الجديدة للاستخدام النظيف للـ Header

        colTeacherAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button();

            {
                FontAwesomeIconView iconDelete = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                iconDelete.setGlyphSize(14);
                iconDelete.setStyle("-fx-fill: white;");
                btnDelete.setGraphic(iconDelete);
                btnDelete.setTooltip(new Tooltip("Delete Teacher"));
                btnDelete.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 5; -fx-cursor: hand;");
                btnDelete.setPrefWidth(50);

                btnDelete.setOnAction(event -> {
                    Teacher teacher = getTableView().getItems().get(getIndex());
                    String fullName = teacher.getName() + " " ;

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, bundle.getString("btn.delete") + " " + fullName + "؟", ButtonType.YES, ButtonType.NO);
                    alert.setTitle(bundle.getString("dialog.confirm.title"));
                    alert.setHeaderText(null);

                    Button yesButton = (Button) alert.getDialogPane().lookupButton(ButtonType.YES);
                    if (yesButton != null) yesButton.setText(bundle.getString("btn.yes"));

                    Button noButton = (Button) alert.getDialogPane().lookupButton(ButtonType.NO);
                    if (noButton != null) noButton.setText(bundle.getString("btn.no"));

                    if (bundle.getLocale().getLanguage().equals("ar")) {
                        alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
                    } else {
                        alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
                    }

                    java.util.Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        teacherService.deleteById(teacher.getId());
                        loadData();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnDelete);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        colEmployeeAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button();

            {
                FontAwesomeIconView iconDelete = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                iconDelete.setGlyphSize(14);
                iconDelete.setStyle("-fx-fill: white;");
                btnDelete.setGraphic(iconDelete);
                btnDelete.setTooltip(new Tooltip("Delete Teacher"));
                btnDelete.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 5; -fx-cursor: hand;");
                btnDelete.setPrefWidth(50);

                btnDelete.setOnAction(event -> {
                    Employee employee = getTableView().getItems().get(getIndex());
                    String fullName = employee.getName() + " " ;

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, bundle.getString("btn.delete") + " " + fullName + "؟", ButtonType.YES, ButtonType.NO);
                    alert.setTitle(bundle.getString("dialog.confirm.title"));
                    alert.setHeaderText(null);

                    Button yesButton = (Button) alert.getDialogPane().lookupButton(ButtonType.YES);
                    if (yesButton != null) yesButton.setText(bundle.getString("btn.yes"));

                    Button noButton = (Button) alert.getDialogPane().lookupButton(ButtonType.NO);
                    if (noButton != null) noButton.setText(bundle.getString("btn.no"));

                    if (bundle.getLocale().getLanguage().equals("ar")) {
                        alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
                    } else {
                        alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
                    }

                    java.util.Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        employeeService.deleteById(employee.getId());
                        loadData();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnDelete);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
    }

    // دالة فتح النافذة المنفصلة وتغذيتها بالبيانات حركياً
    private void openFormWindowEmployee(Employee staff, boolean isTeacherTab) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/stuff-form.fxml"), bundle);
            loader.setControllerFactory(springContext::getBean); // تضمن ربط الـ Core الخاص بـ Spring Boot
            Parent root = loader.load();

            StuffFormController formController = loader.getController();
            // نمرر البيانات والدالة السرجية لتحديث البيانات فور الحفظ تلقائياً
            formController.setEmployeeData(staff, isTeacherTab, this::loadData);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // تجعل الشاشة منبثقة وقافلة للخلفية لقفل التداخل
            stage.setTitle(staff == null ? "إضافة إدخال جديد" : "تعديل البيانات");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}