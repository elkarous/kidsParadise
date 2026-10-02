package com.kindererp.controller;

import com.kindererp.model.Level;
import com.kindererp.model.SchoolClass;
import com.kindererp.model.Student;
import com.kindererp.service.ClassService;
import com.kindererp.service.StudentService;
import com.kindererp.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class StudentListController {

    private final StudentService studentService;
    private final ClassService classService;

    @FXML private TextField searchField;
    @FXML private ComboBox<Level> levelFilter;
    @FXML private ComboBox<SchoolClass> classFilter;
    @FXML private Label countLabel;
    @FXML private TableView<Student> table;
    @FXML private TableColumn<Student, String> lastNameColumn;
    @FXML private TableColumn<Student, String> firstNameColumn;
    @FXML private TableColumn<Student, String> birthDateColumn;
    @FXML private TableColumn<Student, String> levelColumn;
    @FXML private TableColumn<Student, String> classColumn;
    @FXML private TableColumn<Student, String> parentColumn;
    @FXML private TableColumn<Student, Void> deleteColumn;

    private final ObservableList<Student> students = FXCollections.observableArrayList();
    private final FilteredList<Student> filtered = new FilteredList<>(students);

    @FXML
    private void initialize() {
        Tables.text(lastNameColumn, Student::getLastName);
        Tables.text(firstNameColumn, Student::getFirstName);
        Tables.text(birthDateColumn, s -> Formats.date(s.getBirthDate()));
        Tables.text(levelColumn, s -> s.getSchoolClass() == null ? "" : s.getSchoolClass().getLevel().getName());
        Tables.text(classColumn, s -> s.getSchoolClass() == null ? "" : s.getSchoolClass().getName());
        Tables.text(parentColumn, s -> s.getParent() == null ? "" : s.getParent().getFatherName());
        Tables.button(deleteColumn, Icons::trash, "btn.delete", true, this::delete);
        Tables.onDoubleClick(table, this::openForm);
        Tables.placeholder(table, "table.empty");
        table.setItems(filtered);

        ComboBoxes.withAllOption(levelFilter, Level::getName);
        ComboBoxes.withAllOption(classFilter, SchoolClass::getName);
        searchField.textProperty().addListener((obs, old, text) -> applyFilter());
        levelFilter.valueProperty().addListener((obs, old, level) -> applyFilter());
        classFilter.valueProperty().addListener((obs, old, schoolClass) -> applyFilter());
        filtered.addListener((javafx.collections.ListChangeListener<Student>) change -> updateCount());

        load();
    }

    private void load() {
        FxAsync.run(table, () -> new Data(studentService.findAll(), classService.levels(), classService.classes()), data -> {
            students.setAll(data.students());
            List<Level> levels = new ArrayList<>();
            levels.add(null);
            levels.addAll(data.levels());
            List<SchoolClass> classes = new ArrayList<>();
            classes.add(null);
            classes.addAll(data.classes());
            Level selectedLevel = levelFilter.getValue();
            SchoolClass selectedClass = classFilter.getValue();
            levelFilter.getItems().setAll(levels);
            classFilter.getItems().setAll(classes);
            levelFilter.setValue(selectedLevel);
            classFilter.setValue(selectedClass);
            applyFilter();
        });
    }

    private void applyFilter() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        Level level = levelFilter.getValue();
        SchoolClass schoolClass = classFilter.getValue();
        filtered.setPredicate(s -> matches(s, query)
                && (level == null || (s.getSchoolClass() != null && level.equals(s.getSchoolClass().getLevel())))
                && (schoolClass == null || schoolClass.equals(s.getSchoolClass())));
        updateCount();
    }

    private static boolean matches(Student s, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String parent = s.getParent() == null ? "" : s.getParent().getFatherName();
        return (s.getFirstName() + " " + s.getLastName() + " " + parent).toLowerCase(Locale.ROOT).contains(query);
    }

    private void updateCount() {
        countLabel.setText(I18n.get("table.count", filtered.size()));
    }

    @FXML
    private void handleAdd() {
        openForm(new Student());
    }

    private void openForm(Student student) {
        ViewLoader.Dialog<StudentFormController> dialog = ViewLoader.dialog("student_form",
                student.getId() == null ? "students.add" : "students.edit", table.getScene().getWindow());
        dialog.controller().setStudent(student);
        dialog.stage().showAndWait();
        if (dialog.controller().isSaved()) {
            load();
        }
    }

    private void delete(Student student) {
        if (Dialogs.confirm(table.getScene().getWindow(), I18n.get("students.delete.confirm", student.fullName()))) {
            FxAsync.runAction(table, () -> studentService.delete(student.getId()), this::load);
        }
    }

    private record Data(List<Student> students, List<Level> levels, List<SchoolClass> classes) {
    }
}
