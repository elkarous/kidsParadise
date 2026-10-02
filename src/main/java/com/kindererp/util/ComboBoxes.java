package com.kindererp.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.function.Function;

/** ComboBox helpers: display converters and type-to-search. */
public final class ComboBoxes {

    private ComboBoxes() {
    }

    public static <T> StringConverter<T> converter(Function<T, String> toText) {
        return new StringConverter<>() {
            @Override
            public String toString(T item) {
                return item == null ? "" : toText.apply(item);
            }

            @Override
            public T fromString(String text) {
                throw new UnsupportedOperationException();
            }
        };
    }

    /** Filter combo whose {@code null} item reads "All"; the prompt text shows while nothing is selected. */
    public static <T> void withAllOption(ComboBox<T> combo, Function<T, String> toText) {
        javafx.util.Callback<javafx.scene.control.ListView<T>, javafx.scene.control.ListCell<T>> factory =
                list -> new javafx.scene.control.ListCell<>() {
                    @Override
                    protected void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : item == null ? I18n.get("filter.all") : toText.apply(item));
                    }
                };
        combo.setCellFactory(factory);
        combo.setButtonCell(factory.call(null));
    }

    /** Shows enum values through translations: key {@code prefix.VALUE}. */
    public static <E extends Enum<E>> StringConverter<E> enumConverter(String prefix) {
        return converter(value -> I18n.get(prefix + "." + value.name()));
    }

    /**
     * Makes the combo editable with type-to-filter (case-insensitive "contains" on the display text).
     * The selected value stays the real item; free text that matches nothing is cleared on focus loss.
     */
    public static <T> void searchable(ComboBox<T> combo, List<T> items, Function<T, String> toText) {
        ObservableList<T> all = FXCollections.observableArrayList(items);
        FilteredList<T> filtered = new FilteredList<>(all, t -> true);
        StringConverter<T> converter = new StringConverter<>() {
            @Override
            public String toString(T item) {
                return item == null ? "" : toText.apply(item);
            }

            @Override
            public T fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                return all.stream().filter(i -> toText.apply(i).equalsIgnoreCase(text.trim())).findFirst()
                        .orElse(combo.getValue());
            }
        };
        T current = combo.getValue();
        combo.setEditable(true);
        combo.setConverter(converter);
        combo.setItems(filtered);
        combo.setValue(current);

        combo.getEditor().textProperty().addListener((obs, old, text) -> {
            T selected = combo.getValue();
            if (selected != null && toText.apply(selected).equals(text)) {
                filtered.setPredicate(t -> true);
                return;
            }
            String query = text == null ? "" : text.trim().toLowerCase(I18n.locale());
            filtered.setPredicate(t -> query.isEmpty() || toText.apply(t).toLowerCase(I18n.locale()).contains(query));
            if (combo.getEditor().isFocused() && !filtered.isEmpty() && !combo.isShowing()) {
                combo.show();
            }
        });
        combo.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                T value = converter.fromString(combo.getEditor().getText());
                combo.setValue(value);
                combo.getEditor().setText(converter.toString(value));
                filtered.setPredicate(t -> true);
            }
        });
    }
}
