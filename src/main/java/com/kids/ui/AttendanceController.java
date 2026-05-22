package com.kids.ui;


import com.kids.entities.*;

import com.kids.services.AttendanceService;
import com.kids.services.LevelService; // افترض وجوده لجلب الفضاءات
import com.kids.services.SchoolClassService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class AttendanceController {

    @Autowired private AttendanceService attendanceService;
    @Autowired private LevelService levelService;
    @Autowired private SchoolClassService classService;
    @Autowired private ResourceBundle resourceBundle;

    @FXML private ComboBox<Level> comboLevel;
    @FXML private ComboBox<SchoolClass> comboClass;
    @FXML private DatePicker datePicker;
    @FXML private TableView<Attendance> attendanceTable;
    @FXML private TableColumn<Attendance, String> colStudentName;
    @FXML private TableColumn<Attendance, String> colTeacherName;
    @FXML private TableColumn<Attendance, Void> colStatusAction;
    @FXML private TableColumn<Attendance, String> colNotes;

    private final ObservableList<Attendance> attendanceDataList = FXCollections.observableArrayList();

    public AttendanceController() {}

    @FXML
    public void initialize() {
        datePicker.setValue(LocalDate.now()); // ضبط التاريخ تلقائياً على اليوم

        setupComboBoxes();
        setupTableColumns();
    }

    private void setupComboBoxes() {
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

        UiService.makeSearchable(
                comboLevel,
                levelService.findAll(),
                levelConverter,
                Level::getLevelName,
                (level, text) -> level.getLevelName().toLowerCase().contains(text)
        );

        // 2. تصفية ديناميكية: عند اختيار فضاء، يتم شحن المجموعات التابعة له فقط (صباحي/مسائي)
        comboLevel.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                StringConverter<SchoolClass> classConverter = new StringConverter<>() {
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
                attendanceDataList.clear();
            }
        });

        // 3. عند اختيار الفوج أو تغيير التاريخ، يتم تحميل دفتر المناداة فوراً
        comboClass.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> loadAttendanceRegister());
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> loadAttendanceRegister());
    }

    private void setupTableColumns() {
        // 1. عرض اسم التلميذ الثنائي (مع حماية ضد الـ Null)
        colStudentName.setCellValueFactory(cellData -> {
            if (cellData != null && cellData.getValue() != null && cellData.getValue().getStudent() != null) {
                String fullName = cellData.getValue().getStudent().getFirstName() + " " + cellData.getValue().getStudent().getLastName();
                return new SimpleStringProperty(fullName);
            }
            return new SimpleStringProperty(""); // إرجاع نص فارغ بأمان إذا كان السطر لم يُشحن بعد
        });

        // 2. عرض اسم المعلم المباشر المسؤول (مع حماية ضد الـ Null)
        colTeacherName.setCellValueFactory(cellData -> {
            if (cellData != null && cellData.getValue() != null && cellData.getValue().getSchoolClass() != null) {
                SchoolClass sc = cellData.getValue().getSchoolClass();
                return new SimpleStringProperty(sc.getTeacher() != null ? sc.getTeacher().getName() : "---");
            }
            return new SimpleStringProperty("");
        });

        // 3. عمود الملاحظات (تحويل الخلية إلى TextField قابل للتعديل المباشر)
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));
        colNotes.setCellFactory(param -> new TableCell<>() {
            private final TextField txtNote = new TextField();
            {
                txtNote.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    // تأمين جلب الفهرس والسطر لتجنب أي NullPointerException أثناء الكتابة
                    if (!newVal && getTableView() != null && getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        Attendance att = getTableView().getItems().get(getIndex());
                        if (att != null) {
                            att.setNotes(txtNote.getText());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView() == null || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Attendance att = getTableView().getItems().get(getIndex());
                    if (att != null) {
                        txtNote.setText(att.getNotes() != null ? att.getNotes() : "");
                        setGraphic(txtNote);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        // بناء أزرار التبديل الذكية (حاضر / غائب) داخل الجدول
        setupStatusToggleCells();
    }

    private void setupStatusToggleCells() {
        colStatusAction.setCellFactory(param -> new TableCell<>() {
            private final ToggleButton btnStatus = new ToggleButton();

            {
                btnStatus.setPrefWidth(120);
                btnStatus.setOnAction(event -> {
                    // تأمين جلب الفهرس والسطر عند الضغط على الزر
                    if (getTableView() != null && getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        Attendance att = getTableView().getItems().get(getIndex());
                        if (att != null) {
                            if (btnStatus.isSelected()) {
                                att.setStatus(AttendanceStatus.ABSENT);
                                styleAsAbsent(btnStatus);
                            } else {
                                att.setStatus(AttendanceStatus.PRESENT);
                                styleAsPresent(btnStatus);
                            }
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                // 🌟 شرط الحماية الذهبي: إذا كانت الخلية فارغة أو الفهرس خارج نطاق البيانات، اخفِ الزر فوراً
                if (empty || getTableView() == null || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Attendance att = getTableView().getItems().get(getIndex());

                    // التأكد من أن كائن الحضور ليس null قبل قراءة الـ Status
                    if (att != null) {
                        if (att.getStatus() == AttendanceStatus.ABSENT) {
                            btnStatus.setSelected(true);
                            styleAsAbsent(btnStatus);
                        } else {
                            btnStatus.setSelected(false);
                            styleAsPresent(btnStatus);
                        }
                        setGraphic(btnStatus);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void styleAsPresent(ToggleButton btn) {
        btn.setText(resourceBundle.getString("attendance.status.present"));
        btn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
    }

    private void styleAsAbsent(ToggleButton btn) {
        btn.setText(resourceBundle.getString("attendance.status.absent"));
        btn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
    }

    private void loadAttendanceRegister() {
        SchoolClass selectedClass = comboClass.getValue();
        LocalDate selectedDate = datePicker.getValue();

        if (selectedClass != null && selectedDate != null) {
            List<Attendance> register = attendanceService.getAttendanceRegister(selectedClass.getId(), selectedDate);
            attendanceDataList.setAll(register);

            // التحديث الآمن
            attendanceTable.setItems(attendanceDataList);
            attendanceTable.refresh();
        }
    }

    @FXML
    private void handleSaveAttendance() {
        if (attendanceDataList.isEmpty()) {
            return;
        }
        try {
            // حفظ القائمة بالكامل في قاعدة البيانات (سواء إدخال جديد أو تحديث لحالة غيابات قديمة)
            attendanceService.saveAllAttendance(attendanceDataList);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, resourceBundle.getString("attendance.save.success"), ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }
}