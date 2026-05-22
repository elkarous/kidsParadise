package com.kids.ui;

import com.kids.entities.Level;
import com.kids.entities.SchoolClass;
import com.kids.entities.Teacher;
import com.kids.services.LevelService;
import com.kids.services.SchoolClassService;
import com.kids.services.TeacherService; // افترض وجوده لشحن كومبو المعلمين
import com.kids.services.UiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpacesManagementController {

    @Autowired private LevelService levelService;
    @Autowired private SchoolClassService classService;
    @Autowired private TeacherService teacherService;

    // عناصر الفضاءات
    @FXML private TableView<Level> levelTable;
    @FXML private TableColumn<Level, Long> colLevelId;
    @FXML private TableColumn<Level, String> colLevelName;
    @FXML private TextField txtLevelName;

    // عناصر الأفواج
    @FXML private TableView<SchoolClass> classTable;
    @FXML private TableColumn<SchoolClass, Long> colClassId;
    @FXML private TableColumn<SchoolClass, String> colClassName;
    @FXML private TableColumn<SchoolClass, String> colClassLevel;
    @FXML private TableColumn<SchoolClass, String> colClassTeacher;
    @FXML private TextField txtClassName;
    @FXML private ComboBox<Level> comboLevelFilter;
    @FXML private ComboBox<Teacher> comboTeacherFilter;

    private final ObservableList<Level> levelList = FXCollections.observableArrayList();
    private final ObservableList<SchoolClass> classList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTables();
        loadData();

        // مستمع للاختيار من جدول الفضاءات لملء الحقل للتعديل
        levelTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) txtLevelName.setText(newVal.getLevelName());
        });

        // مستمع للاختيار من جدول الأفواج لملء الحقول للتعديل
        classTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtClassName.setText(newVal.getClassName());
                comboLevelFilter.setValue(newVal.getLevel());
                comboTeacherFilter.setValue(newVal.getTeacher());
            }
        });
        StringConverter<Level> levelConverter = new StringConverter<>() {
            @Override
            public String toString(Level l) {
                return l == null ? "" : l.getLevelName();
            }

            @Override
            public Level fromString(String s) {
                return null;
            }
        };

        StringConverter<Teacher> teacherConverter = new StringConverter<>() {
            @Override
            public String toString(Teacher t) {
                return t == null ? "" : t.getName();
            }

            @Override
            public Teacher fromString(String s) {
                return null;
            }
        };

        UiService.makeSearchable(
                comboLevelFilter,
                levelService.findAll(),
                levelConverter,
                Level::getLevelName,
                (level, text) -> level.getLevelName().toLowerCase().contains(text)
        );

        UiService.makeSearchable(
                comboTeacherFilter,
                teacherService.findAll(),
                teacherConverter,
                Teacher::getName,
                (teacher, text) -> teacher.getName().toLowerCase().contains(text)
        );
    }

    private void setupTables() {
        colLevelId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colLevelName.setCellValueFactory(new PropertyValueFactory<>("levelName"));

        colClassId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colClassName.setCellValueFactory(new PropertyValueFactory<>("className"));
        colClassLevel.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLevel().getLevelName()));
        colClassTeacher.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getTeacher() != null ? cell.getValue().getTeacher().getName() : "بدون معلم"
        ));
    }

    private void loadData() {
        levelList.setAll(levelService.findAll());
        levelTable.setItems(levelList);
        classList.setAll(classService.findAll());
        classTable.setItems(classList);

    }

    // ========== عمليات إدارة الفضاءات (Levels) ==========

    @FXML
    private void handleAddLevel() {
        if (txtLevelName.getText().trim().isEmpty()) return;
        Level level = new Level();
        level.setLevelName(txtLevelName.getText().trim());
        levelService.save(level);
        txtLevelName.clear();
        loadData();
    }

    @FXML
    private void handleUpdateLevel() {
        Level selected = levelTable.getSelectionModel().getSelectedItem();
        if (selected == null || txtLevelName.getText().trim().isEmpty()) return;
        selected.setLevelName(txtLevelName.getText().trim());
        levelService.save(selected);
        txtLevelName.clear();
        loadData();
    }

    @FXML
    private void handleDeleteLevel() {
        Level selected = levelTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "هل أنت متأكد من حذف الفضاء؟ سيؤدي ذلك لحذف الأفواج التابعة له.", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            levelService.delete(selected.getId());
            txtLevelName.clear();
            loadData();
        }
    }

    // ========== عمليات إدارة الأفواج (SchoolClasses) ==========

    @FXML
    private void handleAddClass() {
        if (txtClassName.getText().trim().isEmpty() || comboLevelFilter.getValue() == null) return;

        SchoolClass sc = new SchoolClass();
        sc.setClassName(txtClassName.getText().trim());
        sc.setLevel(comboLevelFilter.getValue());
        sc.setTeacher(comboTeacherFilter.getValue());

        classService.save(sc);
        clearClassFields();
        loadData();
    }

    @FXML
    private void handleUpdateClass() {
        SchoolClass selected = classTable.getSelectionModel().getSelectedItem();
        if (selected == null || txtClassName.getText().trim().isEmpty() || comboLevelFilter.getValue() == null) return;

        selected.setClassName(txtClassName.getText().trim());
        selected.setLevel(comboLevelFilter.getValue());
        selected.setTeacher(comboTeacherFilter.getValue());

        classService.save(selected);
        clearClassFields();
        loadData();
    }

    @FXML
    private void handleDeleteClass() {
        SchoolClass selected = classTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "هل تريد حذف هذا الفوج؟", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            classService.deleteById(selected.getId());
            clearClassFields();
            loadData();
        }
    }

    private void clearClassFields() {
        txtClassName.clear();
        comboLevelFilter.getSelectionModel().clearSelection();
        comboTeacherFilter.getSelectionModel().clearSelection();
    }
}