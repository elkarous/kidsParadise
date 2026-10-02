package com.kindererp.controller;

import com.kindererp.service.StaffKind;
import com.kindererp.util.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Teachers / employees tabs, both showing the same staff list view. */
@Component
@Scope("prototype")
public class StaffController {

    @FXML private Tab teachersTab;
    @FXML private Tab employeesTab;

    @FXML
    private void initialize() {
        teachersTab.setContent(listFor(StaffKind.TEACHER));
        employeesTab.setContent(listFor(StaffKind.EMPLOYEE));
    }

    private javafx.scene.Parent listFor(StaffKind kind) {
        ViewLoader.View<StaffListController> view = ViewLoader.load("staff_list");
        view.controller().setKind(kind);
        return view.root();
    }
}
