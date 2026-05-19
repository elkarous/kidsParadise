package com.kids.services;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BiPredicate;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class UiService {

    private final ConfigurableApplicationContext springContext;
    private final ResourceBundle resourceBundle;

    /**
     * دالة ذكية لفتح النوافذ المنبثقة وتطبيق الإعدادات (الاتجاه والأيقونة) تلقائياً
     */
    public FXMLLoader openPopup(String fxmlName, String titleKey, Window ownerWindow, Stage dialogStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlName));
        loader.setControllerFactory(springContext::getBean);
        loader.setResources(resourceBundle);

        Parent page = loader.load();

        // 1. ضبط اتجاه الكتابة (RTL / LTR)
        if (resourceBundle.getLocale().getLanguage().equals("ar")) {
            page.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        } else {
            page.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        }

        // 2. إعدادات الـ Stage الخاص بالـ Popup
        dialogStage.setTitle(resourceBundle.getString(titleKey));
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(ownerWindow);
        dialogStage.setScene(new Scene(page));

        // ================================================================
        // الحل الجذري: تمرير الأيقونة تلقائياً للـ Popup هنا
        // ================================================================
        var iconStream = getClass().getResourceAsStream("/images/app_icon.png");
        if (iconStream != null) {
            dialogStage.getIcons().add(new Image(iconStream));
        }
        // ================================================================

        return loader;
    }




        /**
         * دالة سحرية لتحويل أي ComboBox عادي إلى حقل بحث ذكي وإكمال تلقائي مستقر
         *
         * @param comboBox      عنصر الـ ComboBox المراد تحويله
         * @param dataList      قائمة البيانات القادمة من السيرفيس
         * @param converter     الـ StringConverter الخاص بالـ Entity
         * @param displayText   دالة تحدد كيف يظهر الكائن داخل أسطر القائمة المنسدلة
         * @param filterMatcher دالة التصفية والبحث (تأخذ الكائن والنص المكتوب وتعيد true إذا تطابق)
         * @param <T>           نوع الـ Entity (مثل Parent, Level, Class)
         */
        public static <T> void makeSearchable(ComboBox<T> comboBox,
                                              List<T> dataList,
                                              StringConverter<T> converter,
                                              Function<T, String> displayText,
                                              BiPredicate<T, String> filterMatcher) {

            ObservableList<T> masterData = FXCollections.observableArrayList(dataList);
            FilteredList<T> filteredData = new FilteredList<>(masterData, p -> true);

            comboBox.setItems(filteredData);
            comboBox.setEditable(true);

            // بناء Converter ذكي داخلياً لحل مشكلة الـ Blur نهائياً
            StringConverter<T> wrappedConverter = new StringConverter<T>() {
                @Override
                public String toString(T item) {
                    return converter.toString(item);
                }

                @Override
                public T fromString(String string) {
                    if (string == null || string.trim().isEmpty()) {
                        return null;
                    }
                    // عند الخروج (Blur)، نبحث في القائمة الأصلية عن الكائن الذي يطابق النص تماماً لمنع الـ null
                    return masterData.stream()
                            .filter(item -> converter.toString(item).equalsIgnoreCase(string.trim()))
                            .findFirst()
                            .orElse(comboBox.getSelectionModel().getSelectedItem());
                    // إذا لم يجد تطابقاً تاماً، يحافظ على آخر عنصر كان مختاراً بدلاً من التصفير
                }
            };

            comboBox.setConverter(wrappedConverter);

            // ضبط مظهر الأسطر في القائمة المنسدلة
            comboBox.setCellFactory(cell -> new ListCell<T>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(displayText.apply(item));
                    }
                }
            });

            // آلية البحث والتصفية الذكية
            comboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
                T selected = comboBox.getSelectionModel().getSelectedItem();
                if (selected != null && wrappedConverter.toString(selected).equals(newValue)) {
                    return;
                }

                Platform.runLater(() -> {
                    if (newValue == null || newValue.trim().isEmpty()) {
                        filteredData.setPredicate(p -> true);
                    } else {
                        String filterText = newValue.toLowerCase().trim();
                        filteredData.setPredicate(item -> item != null && filterMatcher.test(item, filterText));

                        if (!filteredData.isEmpty() && comboBox.getEditor().isFocused()) {
                            comboBox.show();
                        }
                    }
                });
            });

            // حماية القيمة عند الاختيار الفعلي
            comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    Platform.runLater(() -> {
                        comboBox.getEditor().setText(wrappedConverter.toString(newVal));
                    });
                }
            });

            // حل إضافي قوي: عند فقدان التركيز (Blur)، نجبر الحقل النصي على تثبيت نص العنصر المختار حالياً
            comboBox.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) { // حدث الـ Blur
                    T safeSelected = comboBox.getSelectionModel().getSelectedItem();
                    if (safeSelected != null) {
                        comboBox.getEditor().setText(wrappedConverter.toString(safeSelected));
                    } else {
                        // إذا لم يكن هناك شيء مختار والنص المكتوب لا يطابق أي عنصر، قم بتصفير الحقل تماماً
                        T matched = wrappedConverter.fromString(comboBox.getEditor().getText());
                        if (matched != null) {
                            comboBox.getSelectionModel().select(matched);
                        } else {
                            comboBox.getEditor().clear();
                            filteredData.setPredicate(p -> true);
                        }
                    }
                }
            });
        }

}