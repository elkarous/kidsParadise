package com.kids.ui;


import com.kids.entities.*;
import com.kids.services.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.textfield.TextFields;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class StudentFormController {

    private final StudentService studentService;
    private final LevelService levelService;
    private final ParentService parentService;
    @Autowired
    private ResourceBundle resourceBundle;    // احذف المتغير القديم txtName واستبدله بالمتغيرات الجديدة:
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private DatePicker dateBirthDate; // أضف هذا للتحكم في تاريخ الميلاد

    @FXML private ComboBox<Level> comboLevel;
    @FXML private ComboBox<SchoolClass> comboClass;
    @FXML private ComboBox<Parent> comboParent;
    @FXML private Label lblFormTitle;

    private Student currentStudent;
    @Getter
    private boolean saveClicked = false;
    @Autowired
    private UiService uiService;

    @FXML
    public void initialize() {
        // ================================================================
        // 1. تطبيق البحث الذكي على الـ comboParent
        // ================================================================
        StringConverter<Parent> parentConverter = new StringConverter<Parent>() {
            @Override public String toString(Parent p) { return p == null ? "" : p.getFatherName() + " " + p.getPhoneNumber(); }
            @Override public Parent fromString(String s) { return null; }
        };

        UiService.makeSearchable(
                comboParent,
                parentService.findAll(),
                parentConverter,
                parent -> parent.getFatherName() + " - " + parent.getPhoneNumber(),
                (parent, text) -> parent.getFatherName().toLowerCase().contains(text) || parent.getPhoneNumber().contains(text)
        );

        // ================================================================
        // 2. تطبيق البحث الذكي على الـ comboLevel
        // ================================================================
        StringConverter<Level> levelConverter = new StringConverter<Level>() {
            @Override public String toString(Level l) { return l == null ? "" : l.getLevelName(); }
            @Override public Level fromString(String s) { return null; }
        };

        UiService.makeSearchable(
                comboLevel,
                levelService.findAll(),
                levelConverter,
                Level::getLevelName,
                (level, text) -> level.getLevelName().toLowerCase().contains(text)
        );

        // ================================================================
        // 3. علاقة الاعتمادية المتسلسلة (تحديث الـ comboClass بأمان)
        // ================================================================
        comboLevel.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getClasses() != null) {
                StringConverter<SchoolClass> classConverter = new StringConverter<SchoolClass>() {
                    @Override public String toString(SchoolClass c) { return c == null ? "" : c.getClassName(); }
                    @Override public SchoolClass fromString(String s) { return null; }
                };

                // بناء البحث الذكي للصفوف بناءً على المستوى المختار
                UiService.makeSearchable(
                        comboClass,
                        new java.util.ArrayList<>(newVal.getClasses()), // تمرير نسخة مستقلة لتجنب مشاكل الهيدرا والـ Lazy Loading
                        classConverter,
                        SchoolClass::getClassName,
                        (cl, text) -> cl.getClassName().toLowerCase().contains(text)
                );
            } else {
                // الحل الجذري والنهائي: استبدال الـ FilteredList المقفلة بقائمة فارغة جديدة تماماً بأمان
                comboClass.setItems(FXCollections.observableArrayList());
                comboClass.getEditor().clear(); // تنظيف النص المكتوب داخل المحرر أيضاً
            }
        });
    }
    @FXML
    private void onSave() {
        currentStudent.setFirstName(txtFirstName.getText());
        currentStudent.setLastName(txtFirstName.getText());
        currentStudent.setBirthDate(dateBirthDate.getValue());
        currentStudent.setSchoolClass(comboClass.getValue());
        currentStudent.setParent(comboParent.getValue());

        studentService.save(currentStudent);
        saveClicked = true;

        // Close popup window
        Stage stage = (Stage) txtFirstName.getScene().getWindow();
        stage.close();
        showAlert(Alert.AlertType.INFORMATION, "msg.saved");
    }

    public void setStudent(Student student) {
        this.currentStudent = student;

        if (student.getId() == null) {
            lblFormTitle.setText(resourceBundle.getString("student.add"));
        } else {
            lblFormTitle.setText(resourceBundle.getString("student.edit.properties"));
            txtFirstName.setText(student.getFirstName());
            txtLastName.setText(student.getLastName());
            dateBirthDate.setValue(student.getBirthDate());
            if (student.getSchoolClass() != null) {
                comboLevel.setValue(student.getSchoolClass().getLevel());
                comboClass.setValue(student.getSchoolClass());
            }
            comboParent.setValue(student.getParent());
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type, resourceBundle.getString(message), ButtonType.OK);
            alert.showAndWait();
        });
    }
}
