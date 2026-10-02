package com.kindererp.controller;

import com.kindererp.model.SchoolSettings;
import com.kindererp.service.SchoolSettingsService;
import com.kindererp.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/** School identity, currency, fees and calendar form (Settings tab, also embedded in the setup wizard). */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class SchoolSettingsController {

    private static final List<String> REGIONS = List.of("TN", "DZ", "MA", "LY", "MR", "EG", "FR", "BE", "CH", "CA");
    private static final long MAX_LOGO_BYTES = 2L * 1024 * 1024;

    private final SchoolSettingsService settingsService;

    @FXML private TextField nameFrField;
    @FXML private TextField nameArField;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> regionCombo;
    @FXML private ImageView logoPreview;
    @FXML private TextField currencyField;
    @FXML private Spinner<Integer> decimalsSpinner;
    @FXML private TextField monthlyFeeField;
    @FXML private TextField discountTwoField;
    @FXML private TextField discountThreeField;
    @FXML private ComboBox<Integer> startMonthCombo;
    @FXML private ComboBox<Integer> endMonthCombo;
    @FXML private HBox saveBar;
    @FXML private Button saveButton;

    private byte[] logo;

    @FXML
    private void initialize() {
        regionCombo.getItems().setAll(REGIONS);
        regionCombo.setConverter(ComboBoxes.converter(code -> Locale.of("", code).getDisplayCountry(I18n.locale()) + " (" + code + ")"));
        decimalsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3, 3));
        List<Integer> months = IntStream.rangeClosed(1, 12).boxed().toList();
        startMonthCombo.getItems().setAll(months);
        endMonthCombo.getItems().setAll(months);
        startMonthCombo.setConverter(ComboBoxes.converter(Formats::monthName));
        endMonthCombo.setConverter(ComboBoxes.converter(Formats::monthName));
        show(AppState.settings());
    }

    /** In the setup wizard the form has no save button of its own. */
    public void setEmbedded(boolean embedded) {
        saveBar.setVisible(!embedded);
        saveBar.setManaged(!embedded);
    }

    public void show(SchoolSettings s) {
        nameFrField.setText(s.getNameFr());
        nameArField.setText(s.getNameAr());
        addressField.setText(s.getAddress());
        phoneField.setText(s.getPhone());
        emailField.setText(s.getEmail());
        regionCombo.setValue(s.getRegion());
        currencyField.setText(s.getCurrencySymbol());
        decimalsSpinner.getValueFactory().setValue(s.getCurrencyDecimals());
        monthlyFeeField.setText(Formats.editableAmount(s.getMonthlyFee()));
        discountTwoField.setText(Formats.editableAmount(s.getDiscountTwoChildren()));
        discountThreeField.setText(Formats.editableAmount(s.getDiscountThreePlusChildren()));
        startMonthCombo.setValue(s.getYearStartMonth());
        endMonthCombo.setValue(s.getYearEndMonth());
        logo = s.getLogo();
        logoPreview.setImage(AppState.logo());
    }

    /** Validates the form and copies it into {@code target}; returns false (after showing errors) if invalid. */
    public boolean applyTo(SchoolSettings target, Window owner) {
        FormValidator validator = new FormValidator();
        validator.check(nameFrField, !nameFrField.getText().isBlank() || !nameArField.getText().isBlank(),
                "validation.required", "school.name");
        validator.phone(phoneField, "school.phone", false);
        validator.email(emailField, "school.email");
        validator.required(regionCombo, "school.region");
        validator.required(currencyField, "school.currency");
        BigDecimal fee = validator.amount(monthlyFeeField, "fees.monthlyFee", false);
        BigDecimal discountTwo = validator.amount(discountTwoField, "fees.discountTwo", false);
        BigDecimal discountThree = validator.amount(discountThreeField, "fees.discountThreePlus", false);
        validator.required(startMonthCombo, "schoolYears.startMonth");
        validator.required(endMonthCombo, "schoolYears.endMonth");
        if (!validator.validate(owner)) {
            return false;
        }
        target.setNameFr(nameFrField.getText());
        target.setNameAr(nameArField.getText());
        target.setAddress(addressField.getText());
        target.setPhone(phoneField.getText());
        target.setEmail(emailField.getText());
        target.setRegion(regionCombo.getValue());
        target.setCurrencySymbol(currencyField.getText().trim());
        target.setCurrencyDecimals(decimalsSpinner.getValue());
        target.setMonthlyFee(fee);
        target.setDiscountTwoChildren(discountTwo);
        target.setDiscountThreePlusChildren(discountThree);
        target.setYearStartMonth(startMonthCombo.getValue());
        target.setYearEndMonth(endMonthCombo.getValue());
        target.setLogo(logo);
        return true;
    }

    @FXML
    private void handleChooseLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("school.logo.choose"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.get("file.images"), "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return;
        }
        try {
            if (Files.size(file.toPath()) > MAX_LOGO_BYTES) {
                Dialogs.warn(window(), I18n.get("school.logo.tooLarge"));
                return;
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            Image image = new Image(new ByteArrayInputStream(bytes));
            if (image.isError()) {
                Dialogs.warn(window(), I18n.get("school.logo.invalid"));
                return;
            }
            logo = bytes;
            logoPreview.setImage(image);
        } catch (IOException e) {
            Dialogs.showError(window(), e);
        }
    }

    @FXML
    private void handleRemoveLogo() {
        logo = null;
        logoPreview.setImage(AppState.productIcon());
    }

    @FXML
    private void handleSave() {
        SchoolSettings settings = AppState.settings();
        if (!applyTo(settings, window())) {
            return;
        }
        FxAsync.run(saveButton, () -> settingsService.save(settings), saved -> {
            AppState.setSettings(saved);
            Screens.updateTitle();
            Dialogs.info(window(), I18n.get("msg.saved"));
        });
    }

    private Window window() {
        return nameFrField.getScene() == null ? null : nameFrField.getScene().getWindow();
    }
}
