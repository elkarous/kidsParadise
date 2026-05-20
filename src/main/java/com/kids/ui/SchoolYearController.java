package com.kids.ui;

import com.kids.entities.SchoolYear;
import com.kids.services.SchoolYearService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SchoolYearController {

    @Autowired private SchoolYearService yearService;

    @FXML private TextField txtYearName;
    @FXML private TableView<SchoolYear> yearTable;
    @FXML private TableColumn<SchoolYear, Long> colId;
    @FXML private TableColumn<SchoolYear, String> colYearName;
    @FXML private TableColumn<SchoolYear, Boolean> colStatus;

    private final ObservableList<SchoolYear> yearList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colYearName.setCellValueFactory(new PropertyValueFactory<>("yearName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("active"));

        loadData();
    }

    private void loadData() {
        yearList.setAll(yearService.getAllYears());
        yearTable.setItems(yearList);
    }

    @FXML
    private void handleCreateYear() {
        String input = txtYearName.getText().trim();
        // التحقق من صحة المدخلات بـ Regular Expression لضمان التنسيق (مثل 2026-2027)
        if (!input.matches("\\d{4}-\\d{4}")) {
            showAlert("خطأ في الصيغة", "يرجى إدخال السنة بالصيغة الصحيحة، مثال: 2026-2027");
            return;
        }

        yearService.createSchoolYearWithMonths(input);
        txtYearName.clear();
        loadData();
        showAlert("برافو!", "تم إطلاق السنة الدراسية وتوليد أشهر الحضور بنجاح من سبتمبر إلى جوان!");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}