package com.kindererp.util;

import com.kindererp.service.Checks;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.TextInputControl;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects form errors, highlights the wrong fields (CSS class {@code field-error}) and shows them
 * all at once. Field names come from translation keys so the messages follow the UI language.
 */
public final class FormValidator {

    private static final String ERROR_CLASS = "field-error";

    private final List<String> problems = new ArrayList<>();
    private final Set<Control> checked = new LinkedHashSet<>();
    private final Set<Control> invalid = new LinkedHashSet<>();

    public FormValidator required(TextInputControl field, String labelKey) {
        return check(field, !Checks.isBlank(field.getText()), "validation.required", labelKey);
    }

    public FormValidator required(ComboBoxBase<?> field, String labelKey) {
        return check(field, field.getValue() != null, "validation.required", labelKey);
    }

    public FormValidator phone(TextInputControl field, String labelKey, boolean required) {
        if (Checks.isBlank(field.getText())) {
            return required ? required(field, labelKey) : mark(field);
        }
        return check(field, Checks.isValidPhone(field.getText()), "validation.phone.format", labelKey);
    }

    public FormValidator email(TextInputControl field, String labelKey) {
        return check(field, Checks.isBlank(field.getText()) || Checks.isValidEmail(field.getText()),
                "validation.email.format", labelKey);
    }

    /** Validates an amount ≥ 0 (or > 0 when {@code strictlyPositive}); returns it, or null if invalid. */
    public BigDecimal amount(TextInputControl field, String labelKey, boolean strictlyPositive) {
        BigDecimal value = Formats.parseAmount(field.getText());
        boolean ok = value != null && (strictlyPositive ? value.signum() > 0 : value.signum() >= 0);
        check(field, ok, strictlyPositive ? "validation.amount.positive" : "validation.amount.format", labelKey);
        return ok ? value : null;
    }

    public FormValidator notInFuture(ComboBoxBase<LocalDate> field, String labelKey) {
        return check(field, field.getValue() == null || !field.getValue().isAfter(LocalDate.now()),
                "validation.date.future", labelKey);
    }

    /** Generic rule; {@code messageKey} receives the translated field label as {0}. */
    public FormValidator check(Control field, boolean ok, String messageKey, String labelKey) {
        mark(field);
        if (!ok) {
            invalid.add(field);
            problems.add(I18n.get(messageKey, I18n.get(labelKey)));
        }
        return this;
    }

    /** Adds a problem not tied to a single field. */
    public FormValidator rule(boolean ok, String messageKey, Object... args) {
        if (!ok) {
            problems.add(I18n.get(messageKey, args));
        }
        return this;
    }

    /** Highlights invalid fields and shows the problems; returns true when the form is valid. */
    public boolean validate(Window owner) {
        checked.forEach(c -> c.getStyleClass().remove(ERROR_CLASS));
        invalid.forEach(c -> c.getStyleClass().add(ERROR_CLASS));
        if (problems.isEmpty()) {
            return true;
        }
        if (!invalid.isEmpty()) {
            invalid.iterator().next().requestFocus();
        }
        Dialogs.validation(owner, problems);
        return false;
    }

    private FormValidator mark(Control field) {
        checked.add(field);
        return this;
    }
}
