package com.kindererp.controller;

import com.kindererp.model.Parent;
import com.kindererp.service.ParentService;
import com.kindererp.service.dto.ParentSummary;
import com.kindererp.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ParentListController {

    private final ParentService parentService;

    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private TableView<ParentSummary> table;
    @FXML private TableColumn<ParentSummary, String> fatherColumn;
    @FXML private TableColumn<ParentSummary, String> motherColumn;
    @FXML private TableColumn<ParentSummary, String> phoneColumn;
    @FXML private TableColumn<ParentSummary, String> childrenColumn;
    @FXML private TableColumn<ParentSummary, String> feeColumn;
    @FXML private TableColumn<ParentSummary, Void> deleteColumn;

    private final ObservableList<ParentSummary> parents = FXCollections.observableArrayList();
    private final FilteredList<ParentSummary> filtered = new FilteredList<>(parents);

    @FXML
    private void initialize() {
        Tables.text(fatherColumn, s -> s.parent().getFatherName());
        Tables.text(motherColumn, s -> s.parent().getMotherName());
        Tables.text(phoneColumn, s -> s.parent().getPhone());
        Tables.amount(childrenColumn, s -> String.valueOf(s.childCount()));
        Tables.amount(feeColumn, s -> Formats.money(s.fee().total()));
        Tables.button(deleteColumn, Icons::trash, "btn.delete", true, this::delete);
        Tables.onDoubleClick(table, s -> openForm(s.parent()));
        Tables.placeholder(table, "table.empty");
        table.setItems(filtered);

        searchField.textProperty().addListener((obs, old, text) -> {
            String query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(s -> query.isEmpty() || (s.parent().getFatherName() + " "
                    + Objects.toString(s.parent().getMotherName(), "") + " " + s.parent().getPhone())
                    .toLowerCase(Locale.ROOT).contains(query));
            countLabel.setText(I18n.get("table.count", filtered.size()));
        });
        load();
    }

    private void load() {
        FxAsync.run(table, parentService::findAllSummaries, list -> {
            parents.setAll(list);
            countLabel.setText(I18n.get("table.count", filtered.size()));
        });
    }

    @FXML
    private void handleAdd() {
        openForm(new Parent());
    }

    private void openForm(Parent parent) {
        ViewLoader.Dialog<ParentFormController> dialog = ViewLoader.dialog("parent_form",
                parent.getId() == null ? "parents.add" : "parents.edit", table.getScene().getWindow());
        dialog.controller().setParent(parent);
        dialog.stage().showAndWait();
        if (dialog.controller().isSaved()) {
            load();
        }
    }

    private void delete(ParentSummary summary) {
        if (Dialogs.confirm(table.getScene().getWindow(), I18n.get("parents.delete.confirm", summary.parent().getFatherName()))) {
            FxAsync.runAction(table, () -> parentService.delete(summary.parent().getId()), this::load);
        }
    }
}
