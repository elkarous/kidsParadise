package com.kindererp.util;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Common TableView column setups, so every screen behaves the same. */
public final class Tables {

    private Tables() {
    }

    public static <T> void text(TableColumn<T, String> column, Function<T, String> value) {
        column.setCellValueFactory(cell -> new SimpleStringProperty(nullToEmpty(value.apply(cell.getValue()))));
    }

    /** Right-aligned numeric/money column. */
    public static <T> void amount(TableColumn<T, String> column, Function<T, String> value) {
        text(column, value);
        column.getStyleClass().add("cell-amount");
    }

    /** A column showing a coloured badge (style class from {@code styleOf}). */
    public static <T> void badge(TableColumn<T, T> column, Function<T, String> text, Function<T, String> styleOf) {
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(text.apply(item));
                badge.getStyleClass().addAll("badge", styleOf.apply(item));
                setGraphic(badge);
            }
        });
        column.getStyleClass().add("cell-center");
    }

    /** A column with an icon button per row (tooltip from {@code tooltipKey}). */
    public static <T> void button(TableColumn<T, Void> column, Supplier<Node> icon, String tooltipKey,
                                  boolean danger, Consumer<T> action) {
        column.setSortable(false);
        column.getStyleClass().add("cell-center");
        column.setCellFactory(c -> new TableCell<>() {
            private final Button button = new Button();

            {
                button.setGraphic(icon.get());
                button.getStyleClass().add("btn-icon");
                if (danger) {
                    button.getStyleClass().add("btn-danger");
                }
                button.setTooltip(new Tooltip(I18n.get(tooltipKey)));
                button.setOnAction(e -> action.accept(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(button));
            }
        });
    }

    public static <T> void onDoubleClick(TableView<T> table, Consumer<T> action) {
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    action.accept(row.getItem());
                }
            });
            return row;
        });
    }

    public static void placeholder(TableView<?> table, String key) {
        table.setPlaceholder(new Label(I18n.get(key)));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
