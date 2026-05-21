package com.kids.ui;

import com.kids.entities.Parent; // تأكد من اسم الـ Entity الخاص بك (مثال: Parent أو ParentEntity)
import com.kids.services.ParentService;
import com.kids.services.TuitionService;
import com.kids.services.UiService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ResourceBundle;

@Component
public class ParentController {

    @Autowired
    private ParentService parentService;

    @Autowired
    private UiService uiService;

    @Autowired
    private TuitionService tuitionService;

    @Autowired
    private ResourceBundle resourceBundle;

    @FXML private TextField txtSearch;
    @FXML private TableView<Parent> parentTable;
    @FXML private TableColumn<Parent, Void> colAction;
    @FXML private TableColumn<Parent, String> colFatherName;
    @FXML private TableColumn<Parent, String> colMotherName;
    @FXML private TableColumn<Parent, String> colPhone;
    @FXML private TableColumn<Parent, String> colStudentCount;
    @FXML private TableColumn<Parent, String> colTotalFees;

    private final ObservableList<Parent> parentList = FXCollections.observableArrayList();
    private FilteredList<Parent> filteredData;

    // كونسلوكتور فارغ إلزامي لـ JavaFX
    public ParentController() {}

    @FXML
    public void initialize() {
        setupTableColumns();
        setupActionHeaderAndCells();
        loadData();
        setupFilter();

        // النقر المزدوج للتعديل
        parentTable.setRowFactory(tv -> {
            TableRow<Parent> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openParentPopup(row.getItem(), parentTable.getScene().getWindow());
                }
            });
            return row;
        });
    }

    private void setupTableColumns() {
        colFatherName.setCellValueFactory(new PropertyValueFactory<>("fatherName"));
        colMotherName.setCellValueFactory(new PropertyValueFactory<>("motherName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
    }

    private void loadData() {
        parentList.setAll(parentService.findAll());
        parentTable.setItems(filteredData != null ? filteredData : parentList);
    }

    // ميزة الفلتر الديناميكي العلوي (البحث أثناء الكتابة)
    private void setupFilter() {
        filteredData = new FilteredList<>(parentList, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(parent -> {
                if (newValue == null || newValue.isBlank()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase().trim();

                if (parent.getFatherName() != null && parent.getFatherName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (parent.getMotherName() != null && parent.getMotherName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else
                    return parent.getPhoneNumber() != null && parent.getPhoneNumber().contains(lowerCaseFilter);// لم يتم العثور على تطابق
            });
        });

        parentTable.setItems(filteredData);
    }

    private void openParentPopup(Parent parent, javafx.stage.Window ownerWindow) {
        try {
            Stage dialogStage = new Stage();
            String titleKey = (parent.getId() == null) ? "btn.add" : "parent.edit.properties";

            // استدعاء البوب اب عبر الـ UiService المجهز بسبرينج
            FXMLLoader loader = uiService.openPopup("parent_form.fxml", titleKey, ownerWindow, dialogStage);

            ParentFormController controller = loader.getController();
            controller.setParent(parent);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                loadData();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupActionHeaderAndCells() {

        // إضافة عمود لعدد الأبناء المسجلين
        colStudentCount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(cellData.getValue().getChildren() != null ? cellData.getValue().getChildren().size() : 0)
        ));

// إضافة عمود للمعلوم الشهري الإجمالي المطلوب من الولي بعد الخصم
        colTotalFees.setCellValueFactory(cellData -> {
            double finalFees = tuitionService.calculateMonthlyFeesForParent(cellData.getValue());
            return new javafx.beans.property.SimpleStringProperty(finalFees + " د.ت");
        });

        // 2. أزرار الحذف داخل الخلايا
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button();
            {
                FontAwesomeIconView iconDelete = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                iconDelete.setGlyphSize(14);
                iconDelete.setStyle("-fx-fill: white;");
                btnDelete.setGraphic(iconDelete);
                btnDelete.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 5; -fx-cursor: hand;");
                btnDelete.setPrefWidth(50);

                btnDelete.setOnAction(event -> {
                    Parent parent = getTableView().getItems().get(getIndex());

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            resourceBundle.getString("btn.delete") + " " + parent.getFatherName() + "؟",
                            ButtonType.YES, ButtonType.NO);
                    alert.setTitle(resourceBundle.getString("dialog.confirm.title"));
                    alert.setHeaderText(null);

                    if (resourceBundle.getLocale().getLanguage().equals("ar")) {
                        alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
                    }

                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            parentService.delete(parent.getId());
                            loadData();
                        }
                    });
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

    public void openParentPopup() {
        Parent parent = new Parent();
        this.openParentPopup(parent, parentTable.getScene().getWindow());
    }
}