package com.kids.ui;

import com.kids.entities.Student;
import com.kids.services.StudentService;
import com.kids.services.UiService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

@Component
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class StudentController {

    @Autowired private final StudentService studentService;
    @Autowired private final UiService uiService;
    @Autowired private final ResourceBundle resourceBundle;

    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, Void> colAction;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colLevel;
    @FXML private TableColumn<Student, String> colClass;
    @FXML private TableColumn<Student, String> colParent;

    // 🌟 عناصر التحكم الجديدة المضافة للبحث والتصفية
    @FXML private TextField txtStudentSearch;
    @FXML private ComboBox<String> comboFilterLevel;
    @FXML private ComboBox<String> comboFilterClass;

    private final ObservableList<Student> studentList = FXCollections.observableArrayList();
    // 🌟 القائمة المغلفة المسؤولة عن التصفية الفورية
    private FilteredList<Student> filteredStudentList;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupActionHeaderAndCells();

        // النقر المزدوج على السطر لفتح التعديل
        studentTable.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    javafx.stage.Window currentWindow = studentTable.getScene().getWindow();
                    openStudentPopup(row.getItem(), currentWindow);
                }
            });
            return row;
        });

        loadData();
        setupSearchAndFilters();
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

        // تحديث فلاتر الـ ComboBox ديناميكياً بناءً على البيانات المتوفرة
        populateFilterComboBoxes();
    }

    // 🌟 دالة ربط المستمعين (Listeners) للحقول لتحديث الجدول تلقائياً بمجرد الكتابة أو الاختيار
    private void setupSearchAndFilters() {
        filteredStudentList = new FilteredList<>(studentList, p -> true);

        txtStudentSearch.textProperty().addListener((observable, oldValue, newValue) -> updateFilterPredicate());
        comboFilterLevel.valueProperty().addListener((observable, oldValue, newValue) -> updateFilterPredicate());
        comboFilterClass.valueProperty().addListener((observable, oldValue, newValue) -> updateFilterPredicate());

        studentTable.setItems(filteredStudentList);
    }

    // 🌟 الخوارزمية الذكية لتصفية أسطر الطلاب
    private void updateFilterPredicate() {
        filteredStudentList.setPredicate(student -> {
            // 1. التصفية عبر شريط البحث النصي (الاسم، اللقب، أو الأب)
            String searchText = txtStudentSearch.getText();
            if (searchText != null && !searchText.isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase().trim();
                boolean matchesFirstName = student.getFirstName() != null && student.getFirstName().toLowerCase().contains(lowerCaseFilter);
                boolean matchesLastName = student.getLastName() != null && student.getLastName().toLowerCase().contains(lowerCaseFilter);
                boolean matchesFatherName = student.getParent() != null && student.getParent().getFatherName() != null && student.getParent().getFatherName().toLowerCase().contains(lowerCaseFilter);

                if (!matchesFirstName && !matchesLastName && !matchesFatherName) {
                    return false;
                }
            }

            // 2. التصفية حسب المستوى التعليمي المختارات
            String selectedLevel = comboFilterLevel.getValue();
            String allText = resourceBundle.containsKey("filter.all") ? resourceBundle.getString("filter.all") : "- الكل -";
            if (selectedLevel != null && !selectedLevel.isEmpty() && !selectedLevel.equals(allText)) {
                String currentLevel = (student.getSchoolClass() != null) ? student.getSchoolClass().getLevel().getLevelName() : "";
                if (!selectedLevel.equals(currentLevel)) return false;
            }

            // 3. التصفية حسب القسم الدراسي المختارات
            String selectedClass = comboFilterClass.getValue();
            if (selectedClass != null && !selectedClass.isEmpty() && !selectedClass.equals(allText)) {
                String currentClass = (student.getSchoolClass() != null) ? student.getSchoolClass().getClassName() : "";
                return selectedClass.equals(currentClass);
            }

            return true; // يظهر السطر إذا اجتاز كل شروط التصفية
        });
    }

    // 🌟 تجميع الأقسام والمستويات الفريدة من البيانات وحقنها في الـ ComboBox
    private void populateFilterComboBoxes() {
        Set<String> levels = new HashSet<>();
        Set<String> classes = new HashSet<>();
        String allText = resourceBundle.containsKey("filter.all") ? resourceBundle.getString("filter.all") : "- الكل -";

        for (Student student : studentList) {
            if (student.getSchoolClass() != null) {
                levels.add(student.getSchoolClass().getLevel().getLevelName());
                classes.add(student.getSchoolClass().getClassName());
            }
        }

        ObservableList<String> levelOptions = FXCollections.observableArrayList(allText);
        levelOptions.addAll(levels);
        comboFilterLevel.setItems(levelOptions);
        comboFilterLevel.setValue(allText);

        ObservableList<String> classOptions = FXCollections.observableArrayList(allText);
        classOptions.addAll(classes);
        comboFilterClass.setItems(classOptions);
        comboFilterClass.setValue(allText);
    }

    // 🌟 تعديل الدالة لتتوافق مع حدث الزر العلوي الجديد في الـ FXML (handleAddNewStudent)
    @FXML
    private void handleAddNewStudent() {
        javafx.stage.Window currentWindow = studentTable.getScene().getWindow();
        openStudentPopup(new Student(), currentWindow);
    }

    private void openStudentPopup(Student student, javafx.stage.Window ownerWindow) {
        try {
            Stage dialogStage = new Stage();
            String titleKey = (student.getId() == null) ? "btn.add" : "student.edit.properties";

            FXMLLoader loader = uiService.openPopup("student_form.fxml", titleKey, ownerWindow, dialogStage);

            StudentFormController controller = loader.getController();
            controller.setStudent(student);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                loadData();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupActionHeaderAndCells() {
        // تحسين الـ Graphics بالـ CSS المتناسق مع الهوية الجديدة للاستخدام النظيف للـ Header

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button();

            {
                FontAwesomeIconView iconDelete = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                iconDelete.setGlyphSize(14);
                iconDelete.setStyle("-fx-fill: white;");
                btnDelete.setGraphic(iconDelete);
                btnDelete.setTooltip(new Tooltip("Delete Student"));
                btnDelete.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 5; -fx-cursor: hand;");
                btnDelete.setPrefWidth(50);

                btnDelete.setOnAction(event -> {
                    Student student = getTableView().getItems().get(getIndex());
                    String fullName = student.getFirstName() + " " + (student.getLastName() != null ? student.getLastName() : "");

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, resourceBundle.getString("btn.delete") + " " + fullName + "؟", ButtonType.YES, ButtonType.NO);
                    alert.setTitle(resourceBundle.getString("dialog.confirm.title"));
                    alert.setHeaderText(null);

                    Button yesButton = (Button) alert.getDialogPane().lookupButton(ButtonType.YES);
                    if (yesButton != null) yesButton.setText(resourceBundle.getString("btn.yes"));

                    Button noButton = (Button) alert.getDialogPane().lookupButton(ButtonType.NO);
                    if (noButton != null) noButton.setText(resourceBundle.getString("btn.no"));

                    if (resourceBundle.getLocale().getLanguage().equals("ar")) {
                        alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
                    } else {
                        alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
                    }

                    java.util.Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        studentService.delete(student);
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
}