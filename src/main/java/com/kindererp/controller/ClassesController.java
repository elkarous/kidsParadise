package com.kindererp.controller;

import com.kindererp.model.Level;
import com.kindererp.model.SchoolClass;
import com.kindererp.model.Teacher;
import com.kindererp.service.ClassService;
import com.kindererp.service.StaffService;
import com.kindererp.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Levels ("spaces") and classes with their responsible teacher. Select a row to edit it, "New" to add. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ClassesController {

    private final ClassService classService;
    private final StaffService staffService;

    @FXML private TableView<Level> levelTable;
    @FXML private TableColumn<Level, String> levelNameColumn;
    @FXML private TextField levelNameField;
    @FXML private Button saveLevelButton;
    @FXML private TableView<SchoolClass> classTable;
    @FXML private TableColumn<SchoolClass, String> classNameColumn;
    @FXML private TableColumn<SchoolClass, String> classLevelColumn;
    @FXML private TableColumn<SchoolClass, String> classTeacherColumn;
    @FXML private TextField classNameField;
    @FXML private ComboBox<Level> classLevelCombo;
    @FXML private ComboBox<Teacher> classTeacherCombo;
    @FXML private Button saveClassButton;

    @FXML
    private void initialize() {
        Tables.text(levelNameColumn, Level::getName);
        Tables.text(classNameColumn, SchoolClass::getName);
        Tables.text(classLevelColumn, c -> c.getLevel().getName());
        Tables.text(classTeacherColumn, c -> c.getTeacher() == null ? I18n.get("class.noTeacher") : c.getTeacher().getName());
        Tables.placeholder(levelTable, "table.empty");
        Tables.placeholder(classTable, "table.empty");
        classLevelCombo.setConverter(ComboBoxes.converter(Level::getName));
        ComboBoxes.withAllOption(classTeacherCombo, Teacher::getName);

        levelTable.getSelectionModel().selectedItemProperty().addListener((obs, old, level) ->
                levelNameField.setText(level == null ? "" : level.getName()));
        classTable.getSelectionModel().selectedItemProperty().addListener((obs, old, c) -> {
            classNameField.setText(c == null ? "" : c.getName());
            classLevelCombo.setValue(c == null ? null : c.getLevel());
            classTeacherCombo.setValue(c == null ? null : c.getTeacher());
        });
        load();
    }

    private void load() {
        FxAsync.run(classTable, () -> new Data(classService.levels(), classService.classes(), staffService.teachers()), data -> {
            levelTable.getItems().setAll(data.levels());
            classTable.getItems().setAll(data.classes());
            classLevelCombo.getItems().setAll(data.levels());
            List<Teacher> teachers = new ArrayList<>();
            teachers.add(null);
            teachers.addAll(data.teachers());
            classTeacherCombo.getItems().setAll(teachers);
        });
    }

    @FXML
    private void handleNewLevel() {
        levelTable.getSelectionModel().clearSelection();
        levelNameField.clear();
        levelNameField.requestFocus();
    }

    @FXML
    private void handleSaveLevel() {
        if (!new FormValidator().required(levelNameField, "class.level").validate(window())) {
            return;
        }
        Level selected = levelTable.getSelectionModel().getSelectedItem();
        Level level = selected != null ? selected : new Level();
        level.setName(levelNameField.getText());
        FxAsync.run(saveLevelButton, () -> classService.saveLevel(level), saved -> {
            handleNewLevel();
            load();
        });
    }

    @FXML
    private void handleDeleteLevel() {
        Level level = levelTable.getSelectionModel().getSelectedItem();
        if (level != null && Dialogs.confirm(window(), I18n.get("class.level.delete.confirm", level.getName()))) {
            FxAsync.runAction(levelTable, () -> classService.deleteLevel(level.getId()), this::load);
        }
    }

    @FXML
    private void handleNewClass() {
        classTable.getSelectionModel().clearSelection();
        classNameField.clear();
        classNameField.requestFocus();
    }

    @FXML
    private void handleSaveClass() {
        FormValidator validator = new FormValidator()
                .required(classNameField, "class.name")
                .required(classLevelCombo, "class.level");
        if (!validator.validate(window())) {
            return;
        }
        SchoolClass selected = classTable.getSelectionModel().getSelectedItem();
        SchoolClass schoolClass = selected != null ? selected : new SchoolClass();
        schoolClass.setName(classNameField.getText());
        schoolClass.setLevel(classLevelCombo.getValue());
        schoolClass.setTeacher(classTeacherCombo.getValue());
        FxAsync.run(saveClassButton, () -> classService.saveClass(schoolClass), saved -> {
            handleNewClass();
            load();
        });
    }

    @FXML
    private void handleDeleteClass() {
        SchoolClass schoolClass = classTable.getSelectionModel().getSelectedItem();
        if (schoolClass != null && Dialogs.confirm(window(), I18n.get("class.delete.confirm", schoolClass.getName()))) {
            FxAsync.runAction(classTable, () -> classService.deleteClass(schoolClass.getId()), this::load);
        }
    }

    private javafx.stage.Window window() {
        return classTable.getScene().getWindow();
    }

    private record Data(List<Level> levels, List<SchoolClass> classes, List<Teacher> teachers) {
    }
}
