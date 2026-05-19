package com.kids.ui;

import com.kids.entities.Student;
import com.kids.services.StudentService;
import com.kids.services.UiService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jdk.jshell.execution.Util;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class StudentController {

    private final StudentService studentService;
    private final UiService uiService;
    private final ApplicationContext springContext;
    private final ResourceBundle resourceBundle;

    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, Void> colAction; // Void type for custom graphic column
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colLevel;
    @FXML private TableColumn<Student, String> colClass;
    @FXML private TableColumn<Student, String> colParent;

    private ObservableList<Student> studentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupActionHeaderAndCells();

        // Double click on a row to edit
        studentTable.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    javafx.stage.Window currentWindow = studentTable.getScene().getWindow();
                    openStudentPopup(row.getItem(),currentWindow);
                }
            });
            return row;
        });

        loadData();
    }

    private void setupTableColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("firstName"));

        colLevel.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getSchoolClass() != null ?
                        cellData.getValue().getSchoolClass().getLevel().getLevelName() : ""
        ));

        colClass.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getSchoolClass() != null ?
                        cellData.getValue().getSchoolClass().getClassName() : ""
        ));

        colParent.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getParent() != null ?
                        cellData.getValue().getParent().getFatherName() : ""
        ));
    }


    private void loadData() {
        studentList.setAll(studentService.findAll());
        studentTable.setItems(studentList);
    }

    private void openStudentPopup(Student student, javafx.stage.Window ownerWindow) {
        try {
            Stage dialogStage = new Stage();

            // تحديد مفتاح الترجمة المناسب للعنوان بناءً على حالة العملية (إضافة أو تعديل)
            String titleKey = (student.getId() == null) ? "btn.add" : "student.edit.properties";

            // استدعاء الخدمة المحدثة (ستتولى ضبط الاتجاه وتعيين الأيقونة تلقائياً للـ dialogStage)
            FXMLLoader loader = uiService.openPopup("student_form.fxml", titleKey, ownerWindow, dialogStage);

            // جلب الـ Controller وتمرير البيانات كالمعتاد
            StudentFormController controller = loader.getController();
            controller.setStudent(student);

            // عرض النافذة وانتظار إغلاقها
            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                loadData();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void setupActionHeaderAndCells() {
        // ==========================================
        // 1. SETUP ADD BUTTON WITH FONT AWESOME ICON
        // ==========================================
        Button btnAdd = new Button();

        // Create the Plus icon
        FontAwesomeIconView iconAdd = new FontAwesomeIconView(FontAwesomeIcon.PLUS);
        iconAdd.setGlyphSize(14);
        iconAdd.setStyle("-fx-fill: white;"); // <-- Use CSS instead of setFill()
        btnAdd.setGraphic(iconAdd);
        btnAdd.setTooltip(new Tooltip("Add New Student")); // Shows helper text on hover
        btnAdd.setStyle("-fx-background-radius: 5; -fx-cursor: hand;");
        btnAdd.setPrefWidth(50);

        btnAdd.setOnAction(e -> {
            javafx.stage.Window currentWindow = btnAdd.getScene().getWindow();

        openStudentPopup(new Student(), currentWindow);});
        colAction.setGraphic(btnAdd);

        // ==========================================
        // 2. SETUP DELETE BUTTON WITH FONT AWESOME ICON
        // ==========================================
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button();

            {
                // Create the Trash Can icon
                FontAwesomeIconView iconDelete = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                iconDelete.setGlyphSize(14);
                iconAdd.setStyle("-fx-fill: white;"); // <-- Use CSS instead of setFill()
                btnDelete.setGraphic(iconDelete);
                btnDelete.setTooltip(new Tooltip("Delete Student"));
                btnDelete.setStyle(" -fx-background-radius: 5; -fx-cursor: hand;");
                btnDelete.setPrefWidth(50);

                btnDelete.setOnAction(event -> {
                    Student student = getTableView().getItems().get(getIndex());
                    String fullName = student.getFirstName() + " " + student.getLastName();
                    // 1. إنشاء نافذة التنبيه مع دمج نص السؤال
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, resourceBundle.getString("btn.delete") + " " + fullName + "؟", ButtonType.YES, ButtonType.NO);

// 2. ترجمة عنوان النافذة (Title)
                    alert.setTitle(resourceBundle.getString("dialog.confirm.title"));
                    alert.setHeaderText(null); // لإخفاء النص الفرعي الافتراضي وجعل التصميم أنظف

// 3. ترجمة الأزرار الداخلية (YES & NO) ديناميكياً
                    Button yesButton = (Button) alert.getDialogPane().lookupButton(ButtonType.YES);
                    if (yesButton != null) {
                        yesButton.setText(resourceBundle.getString("btn.yes"));
                    }

                    Button noButton = (Button) alert.getDialogPane().lookupButton(ButtonType.NO);
                    if (noButton != null) {
                        noButton.setText(resourceBundle.getString("btn.no"));
                    }

// 4. ضبط اتجاه الكتابة تلقائياً (RTL للعربية و LTR للبقية)
                    if (resourceBundle.getLocale().getLanguage().equals("ar")) {
                        alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
                    } else {
                        alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
                    }

// 5. عرض النافذة وانتظار قرار المستخدم
                    java.util.Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        studentService.delete(student);
                        loadData();                    }
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
                    setAlignment(javafx.geometry.Pos.CENTER); // Centers the button inside the cell
                }
            }
        });
    }
}