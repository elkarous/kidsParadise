package com.kids.ui;

import com.kids.entities.Level;
import com.kids.entities.Parent;
import com.kids.entities.SchoolClass;
import com.kids.entities.Student;
import com.kids.services.LevelService;
import com.kids.services.ParentService;
import com.kids.services.StudentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final LevelService levelService;
    private final ParentService parentService;

    // Table mapping from Template
    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colLevel;
    @FXML private TableColumn<Student, String> colClass;
    @FXML private TableColumn<Student, String> colParent;

    // Form mapping from Template
    @FXML private TextField txtName;
    @FXML private ComboBox<Level> comboLevel;
    @FXML private ComboBox<SchoolClass> comboClass;
    @FXML private ComboBox<Parent> comboParent;

    private ObservableList<Student> studentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Column Factories
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));

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

        // 2. Load initial data into ComboBoxes
        comboLevel.setItems(FXCollections.observableArrayList(levelService.findAllWithClasses()));
        comboParent.setItems(FXCollections.observableArrayList(parentService.findAll()));

        // 3. Cascading Logic: Filter Class based on Level selection
        comboLevel.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                comboClass.setItems(FXCollections.observableArrayList(newVal.getClasses()));
            } else {
                comboClass.getItems().clear();
            }
        });

        // 4. Selection Logic: Populate form when table row is clicked
        studentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtName.setText(newVal.getFirstName() + " " + newVal.getLastName());
                comboLevel.setValue(newVal.getSchoolClass().getLevel());
                comboClass.setValue(newVal.getSchoolClass());
                comboParent.setValue(newVal.getParent());
            }
        });

        loadData();
    }

    private void loadData() {
        studentList.setAll(studentService.findAll());
        studentTable.setItems(studentList);
    }

    @FXML
    private void onSave() {
        // Get selected student if updating, otherwise create new
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        Student student = (selected != null) ? selected : new Student();

        student.setFirstName(txtName.getText());
        student.setSchoolClass(comboClass.getValue());
        student.setParent(comboParent.getValue());
        student.setFullName(student.getFirstName() + " " + student.getLastName());

        studentService.save(student);
        loadData();
        clearForm();
    }

    @FXML
    private void onDelete() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            studentService.delete(selected);
            loadData();
            clearForm();
        }
    }

    @FXML
    private void clearForm() {
        txtName.clear();
        comboLevel.setValue(null);
        comboClass.setValue(null);
        comboParent.setValue(null);
        studentTable.getSelectionModel().clearSelection();
    }
}