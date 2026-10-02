package com.kindererp.controller;

import com.kindererp.model.Parent;
import com.kindererp.model.PaymentMethod;
import com.kindererp.model.TuitionPayment;
import com.kindererp.model.WorkingMonth;
import com.kindererp.service.PaymentService;
import com.kindererp.util.ComboBoxes;
import com.kindererp.util.Formats;
import com.kindererp.util.FxAsync;
import com.kindererp.util.PrintSupport;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Confirms a family's monthly tuition payment; the amount is computed by the tuition rule. */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class PaymentDialogController {

    private final PaymentService paymentService;

    @FXML private Label parentLabel;
    @FXML private Label monthLabel;
    @FXML private Label childrenLabel;
    @FXML private Label baseLabel;
    @FXML private Label discountLabel;
    @FXML private Label totalLabel;
    @FXML private ComboBox<PaymentMethod> methodCombo;
    @FXML private TextField notesField;
    @FXML private CheckBox printCheck;
    @FXML private Button confirmButton;

    private Parent parent;
    private WorkingMonth month;
    @Getter
    private TuitionPayment payment;

    @FXML
    private void initialize() {
        methodCombo.getItems().setAll(PaymentMethod.values());
        methodCombo.setConverter(ComboBoxes.enumConverter("paymentMethod"));
        methodCombo.setValue(PaymentMethod.CASH);
    }

    public void setData(Parent parent, WorkingMonth month) {
        this.parent = parent;
        this.month = month;
        parentLabel.setText(parent.getFatherName());
        monthLabel.setText(Formats.month(month));
        FxAsync.run(confirmButton, () -> paymentService.feeFor(parent.getId()), fee -> {
            childrenLabel.setText(String.valueOf(fee.childCount()));
            baseLabel.setText(Formats.money(fee.baseAmount()));
            discountLabel.setText(Formats.money(fee.discount()));
            totalLabel.setText(Formats.money(fee.total()));
        });
    }

    @FXML
    private void handleConfirm() {
        FxAsync.run(confirmButton,
                () -> paymentService.registerPayment(parent.getId(), month.getId(), methodCombo.getValue(), notesField.getText()),
                saved -> {
                    payment = saved;
                    Stage stage = (Stage) confirmButton.getScene().getWindow();
                    stage.close();
                    if (printCheck.isSelected()) {
                        PrintSupport.printTuitionReceipt(stage.getOwner(), saved);
                    }
                });
    }

    @FXML
    private void handleCancel() {
        ((Stage) confirmButton.getScene().getWindow()).close();
    }
}
